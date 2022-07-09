package com.ryou.tomcat.net;

import ch.qos.logback.classic.Logger;
import com.ryou.tomcat.net.util.NioBufferHandler;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/7 12:09
 * Server socket acceptor thread. 接收socket连接的线程
 */
public class Acceptor implements Runnable {

    private final static Logger log = (Logger) LoggerFactory.getLogger(Acceptor.class);
    /**
     * 读写缓冲区的大小
     */
    protected int readBufSize = 8192;
    protected int writeBufSize = 8192;

    private final NioEndpoint endpoint;

    public Acceptor(NioEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void run() {
        /* 如果endpoint正在运行，就可以阻塞接受请求*/
        log.info("在 -- [{}] -- 端口开启等待请求服务", endpoint.getPort());
        while (endpoint.isRunning()) {
            SocketChannel socket = null;
            try {
                // 获取一个连接
                socket = endpoint.accept();
                // 阻塞等待 socket 连接 ===> 前面把 ServerSocketChannel设置为阻塞的了
                // 把这个套接字交由适当的处理器
                if (endpoint.isRunning() && socket != null) processSocket(socket);
            }
            catch (IOException e) {
                log.info("endpoint.accept.fail", e);
                try {
                    // 发生异常，释放一个连接名称，断开连接
                    // 这里的异常一般是由客户端连接后立刻又断开引起
                    if (socket != null) {
                        socket.socket().close();
                        socket.close();
                    }
                } catch (IOException ignore) {
                }
            }
        }

    }

    private void processSocket(SocketChannel socket) throws IOException {
        // 设置成非阻塞模式
        socket.configureBlocking(false);
        // TCP连接会延迟
        socket.socket().setTcpNoDelay(true);
        // 此连接的超时时间
        socket.socket().setSoTimeout(endpoint.getSoTimeout());

        // TODO NioChannel省去了 先从缓存区读取，  缓存区就是一个队列
        // 封装成 NioChannel 对象
        NioBufferHandler bufferHandler = new NioBufferHandler(12532305, writeBufSize);
        NioChannel channel = new NioChannel(socket, bufferHandler);
        // 将 NioChannel 对象插入 poller 的队列中，并指定关注的事件,getPoller0()：多个poller随机传来一个
        endpoint.getPoller0().register(channel, SelectionKey.OP_READ);
        log.info("-----------接收通道 [{}] 连接-----------", channel);
    }
}
