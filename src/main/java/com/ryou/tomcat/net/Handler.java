package com.ryou.tomcat.net;

import ch.qos.logback.classic.Logger;
import com.ryou.tomcat.net.util.SocketState;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/7 12:09
 *
 * 管理着通道和处理器的映射
 */
public abstract class Handler {

    private final static Logger log = (Logger) LoggerFactory.getLogger(Acceptor.class);

    /** 连接和处理器的映射，主要是非阻塞读或写不完整时，再次处理时关联旧的处理器 */
    private final Map<NioChannel, Processor> connections = new ConcurrentHashMap<>();

    /**
     * 对通道进行真正的处理
     * @param socket
     * @return
     */
    public SocketState process(NioChannel socket) {

        // 如果通道是关闭的，直接返回，不需要进行操作
        if (!socket.isOpen()) return SocketState.CLOSED;

        log.info("进行连接操作 - [{}]", socket);
        // 是否存在关联的 processor
        Processor processor = connections.get(socket);
        if (Objects.isNull(processor)) {
            /** 创建一个处理类实例，并且放入connections连接管理器里面 */
            processor = createProcessor();
            connections.put(socket, processor);
            log.debug("为通道 [{}] 创建新的 Processor [{}]", socket, processor);
        } else {
            log.debug("获取通道 [{}] 已创建关联的 Processor [{}]", socket, processor);
        }
        // 处理的状态
        SocketState state;
        // 调用 Processor(正在的处理器如：HttpNioProcessor、Http11Processor、JioProcessor) 处理
        state = processor.process(socket);

        if (state == SocketState.LONG) {
            log.debug("[请求头数据不完整]，通道 [{}] 重新声明关注 [读取] 事件", socket);
            // 处理期间发现读取的数据不完整，要再次读取，此时通道要再次在 Poller 上声明关注读取事件
            socket.getPoller().register(socket, SelectionKey.OP_READ);
            // 不会移除通道和处理器的映射关系
        }
        else if (state == SocketState.OPEN) {
            log.debug("[保持连接]，通道 [{}] 重新声明关注 [读取] 事件", socket);
            // 长连接，要保持连接，因为不知道下次请求的时间，所以可以回收利用此通道关联的 Processor
            // 这里主要是模拟实现，并没有真正实现 Processor 对象池
            connections.remove(socket);
            // 再次声明关注读取事件
            socket.getPoller().register(socket, SelectionKey.OP_READ);
        }
        else if (state == SocketState.WRITE) {
            log.debug("[写入响应数据]，通道 [{}] 声明关注 [写入] 事件", socket);
            // 简单起见，这个 Poller 也处理写入事件
            socket.getPoller().register(socket, SelectionKey.OP_WRITE);
        }
        else {  // 关闭连接
            connections.remove(socket);
        }

        return state;
    }

    /**
     * 通道超时或关闭时移除对应的 Processor，防止内存泄露
     *
     * @param socket NioChannel
     */
    public void release(NioChannel socket) {
        Processor p = connections.remove(socket);
        if (p != null) {
            log.debug("释放通道 [{}] 关联的 Processor [{}]", socket, p);
        }
    }


    /**
     * 交由 真正的实现类 去创建一个合适的处理类
     * 如 创建一个HttpNioProcessor,来处理http请求和响应
     * @return
     */
    protected abstract Processor createProcessor();
}

