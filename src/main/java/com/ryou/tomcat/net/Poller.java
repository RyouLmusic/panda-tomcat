package com.ryou.tomcat.net;


import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/7 12:10
 * 轮询器：将Acceptor获得的连接封装成NioChannel之后，在此处进行 处理
 * + selector进行各自的事件进行分配
 * + 超时处理
 */
public class Poller implements Runnable {
    final static Logger log = (Logger) LoggerFactory.getLogger(Poller.class);
    /**
     * 源码中Poller是内部类，这里，需要NioEndPoint里面的东西
     */
    private final NioEndpoint endPoint;
    /**
     * 每个poller里面都有一个selector选择器 核心
     */
    private final Selector selector;
    // events 队列，此类的核心
    // 此处直接存储
    private final ConcurrentLinkedQueue<PollerEvent> events = new ConcurrentLinkedQueue<>();
    private volatile boolean close = false;
    /** 下次检查通道是否超时的时间，保证有一个最小时间间隔 */
    protected long nextExpiration = 0;//optimize expiration handling

    public Poller(NioEndpoint endPoint) throws IOException {
        this.endPoint = endPoint;
        // 复用器开启
        selector = Selector.open();
    }

    /**
     * 该方法会一直循环，直到 poller.destroy() 被调用。
     * 在此处轮询 selector里面所注册的channel，进行分别处理channel的读写事件
     */
    // poller线程的运行方法
    @Override
    public void run() {
        int keyCount = 0;
        while (true) {
            try {
                // 如果没有关闭
                if (!close) {
                    // 执行 events 队列中每个 event 的 run() 方法
                    boolean hasEvent = event(); // 如果select(100)的阻塞时间过长，就会导致 这里的event无法更新，也就是导致selector的没有注册进来事件
                    // 如果在selector选择器里面注册一个 OP_ACCEPT 事件的ServerSocketChannel，就可以将 selector.select()，无限阻塞
                    // 这样 每当请求来了的时候，也可以唤醒 selector选择器

                    // 但是如果不注册一个ServerSocketChannel的话，只能进行空循环了，而且由于 selector注册事件发生在event里面
                    // 所以在每个请求来了的时候，即在register方法里面调用 selector.wakeup()方法，唤醒selector
                    // 这样就不用设置为 很短时间循环一次，导致资源的浪费

                    // 已经有多少个读写事件需要进行处理
                    keyCount = selector.select(5000); // 此方法会阻塞 超过5000毫秒的时间，就会跳出阻塞
                }
                // 如果poller已经关闭了
                else if (close) { // 处理关闭
                    timeout(0);
                    break;
                }
            } catch (IOException e) {
                log.error("", e);
                // 进行记录之后，继续运行队列里面的事件events
                continue;
            }
            // 如果刚刚 select 有返回 ready keys，进行处理
            Iterator<SelectionKey> iterator =
                    keyCount > 0 ? selector.selectedKeys().iterator() : null;

            if (iterator == null) continue;
            while (iterator.hasNext()) {
                SelectionKey sk = iterator.next();
                NioChannel attachment = (NioChannel) sk.attachment();
                iterator.remove();

                // 处理就绪的通道
                if (attachment != null && sk.isValid() && (sk.isReadable() || sk.isWritable())) {
                    // 调用endPoint里面的方法进行真正的 处理
                    /** 因为这里是直接将通道给线程池处理的，所以，并不会阻塞在此处 */
                    /**
                     * 返回true只是表明 此通道被 放入 线程池进行处理而已，并没有说明此通道可以被完美处理
                     */
                    log.info("*********处理来自客户端的连接  [{}]", attachment);
                    if (!endPoint.processSocket(attachment)) {
                        try {
                            attachment.close();
                            sk.cancel();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // 立即将sk设置为连接状态，防止多次进行循环
                    sk.interestOps(SelectionKey.OP_CONNECT);
                }
            }
            // 检查连接是否超时
            timeout(keyCount);
        }
    }

    /**
     * 检查是否有通道读写超时
     * 如果发生了超时，就进行关闭通道
     * @param keyCount int
     */
    private void timeout(int keyCount) {
        // 获取系统当前时间
        long nowTime = System.currentTimeMillis();
        // 如果发生以下情况，就不需要进行关闭操作
        // 1.selector里面注册的channel数小于零 或者 events队列里面没有事务
        // 2.对比上一次的检查时间 还未超过
        // 3.poller还未关闭
        if (nextExpiration > 0 && (keyCount > 0) && (nowTime < nextExpiration)) {
            return;
        }
        for (SelectionKey key : selector.keys()) {

            try {
                NioChannel channel = (NioChannel) key.attachment();
                // 如果附加对象，直接关闭通道
                if (channel == null) cancelledKey(key);
                else if (key.interestOps() == SelectionKey.OP_READ || key.interestOps() == SelectionKey.OP_WRITE) {
                    // 仅仅检测当前关注读或写的通道
                    // getLastAccess()获取到的是  channel创建的时间
                    long delta = nowTime - channel.getLastAccess();
                    boolean isTimedOut = delta > endPoint.getSoTimeout();
                    if (isTimedOut) {
                        log.debug("通道 [{}] 读或写超时", channel);
                        // 超时关闭连接
                        key.interestOps(SelectionKey.OP_CONNECT);
                        cancelledKey(key);
                        // 唤醒阻塞中的selector
                        selector.wakeup();
                    }
                }
            }
            catch (CancelledKeyException ckx) {
                log.debug("", ckx);
                cancelledKey(key);
            }
        }

        // 设置下一次检查的时间点 TODO endPoint.socketProperties();来获取配置数据
        nextExpiration = nowTime + 1000;

    }

    /**
     * 这里就直接将NioChannel加入events里面了
     * TODO 通过NioSocketWrapper对NioChannel的封装，最后将NioSocketWrapper封装在PollerEvent中，最后加到PollerEvent队列
     * @param socket NioChannel
     * @param interestOps int 注册操作事件
     */
    public void register(NioChannel socket, int interestOps) {
        // 将轮询器传送给 socket
        socket.setPoller(this);
        PollerEvent event = new PollerEvent(socket, interestOps);

        //
        events.add(event);

        /** 有请求来 ，就唤醒 event*/
        // 唤醒 Selector 进行处理
        selector.wakeup();
    }


    /**
     *
     * @return
     */
    private boolean event () {
        boolean result = false;
        for (PollerEvent event : events) {
            result = true;
            event.run();
            events.remove(event);
        }
        return result;
    }


    /**
     * 销毁 ，将close设置为true，此poller就不会 继续循环下去了，而是直接跳出，停止运行
     */
    public void destroy() throws IOException {
        close = true;
        selector.close();
    }


    /**
     * 关闭 此次连接，poller还是存在，并且释放了 连接管理器里面的映射关系
     * @param key SelectionKey
     */
    public void cancelledKey(SelectionKey key) {
        // attach()里面是getAndSet()方法
        NioChannel socket = (NioChannel) key.attach(null);
        if (socket != null) {
            // 释放连接可能占用的 Processor
            // 清理 存储在Handler里面的映射器 Map<NioChannel, Processor> connections 的channel
            endPoint.getHandler().release(socket);
            // 取消 key
            if (key.isValid())
                key.cancel();
            log.debug("关闭通道 [{}] 连接", socket);
            // 关闭连接
            try {
                socket.close();
            }
            catch (IOException e) {
                log.debug("Channel close failed", e);
            }
            // 释放一个连接名额
        }
    }

    // --------------------------------------------------  PollerEvent
    /**
     *
     */
    class PollerEvent {
        NioChannel socket;
        int ops;
        public PollerEvent(NioChannel socket, int ops) {
            this.socket = socket;
            this.ops = ops;
        }

        public void run() {
            try {
                socket.getIOChannel().register(selector, ops, socket);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        }
    }

}
