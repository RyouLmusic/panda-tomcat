package com.ryou.tomcat.net;

import ch.qos.logback.classic.Logger;
import com.ryou.tomcat.net.util.NioBufferHandler;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/7 12:09
 */
public class NioChannel {

    private final static Logger log = (Logger) LoggerFactory.getLogger(Acceptor.class);

    private Poller poller;
    // 接收连接之后的新 channel
    private final SocketChannel socket;
    /**
     * CountDownLatch是一个同步工具类，它允许一个或多个线程一直等待，直到其他线程执行完后再执行。
     */
    /** 读线程等待，readLatch.await,就是等待，readLatch = new CountDownLatch(1);readLatch.countDown();
     * readLatch.getCount() == 0的时候，调用await的线程开始执行*/
    private CountDownLatch readLatch;
    private CountDownLatch writeLatch;

    /**
     * NioChannel实例化的时间
     * 最后操作的时间
     */
    private long lastAccess = -1;


    // 里面包含读写的 缓冲buffer
    NioBufferHandler bufferHandler;
    public NioChannel(SocketChannel socket, NioBufferHandler bufferHandler) {
        this.socket = socket;
        this.bufferHandler = bufferHandler;
        lastAccess = System.currentTimeMillis();
        access();
    }


    /**
     * 建立起跟poller的联系
     * @param poller Poller 是这个轮询器 对其进行处理的
     */
    public void setPoller(Poller poller) {
        this.poller = poller;
    }

    /**
     * 获取连接的channel
     * @return SocketChannel
     */
    public SocketChannel getIOChannel() {
        return socket;
    }

    public void close() throws IOException {
        getIOChannel().socket().close();
        getIOChannel().close();
    }

    public boolean isOpen() {
        return getIOChannel().isOpen();
    }

    /**
     * 强制关闭
     * @param force boolean
     */
    public void close(boolean force) throws IOException {
        if (isOpen() || force ) close();
    }

    /**
     * 在此通道进行写操作
     * @param src ByteBuffer 将此缓冲区的内容从此通道写出去
     * @throws IOException
     */
    public int write(ByteBuffer src) throws IOException {
        // src.remaining()： position和limit之间的间隔数，就是可以被get的数据还有多少
        while (src.hasRemaining()) {
            getIOChannel().write(src);
        }

        return src.remaining();
    }

    /**
     * 在此通道进行读操作
     * @param dst ByteBuffer 读到此缓冲区里面
     * @throws IOException
     */
    public int read(ByteBuffer dst) throws IOException {
        dst.clear(); // 清理buffer
        return getIOChannel().read(dst);
    }

    /**
     * 获取读操作的缓冲区
     * @return ByteBuffer
     */
    public ByteBuffer readBuf() {
        return bufferHandler.getReadBuffer();
    }

    /**
     * 获取写缓冲区
     * @return ByteBuffer
     */
    public ByteBuffer writeBuf() {
        return bufferHandler.getWriteBuffer();
    }

    /**
     * 更新lastAccess
     */
    public void access() {
        lastAccess = System.currentTimeMillis();
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public Poller getPoller() {
        return poller;
    }


    @Override
    public String toString() {
        return getIOChannel().socket().getRemoteSocketAddress().toString();
    }

    /**
     * 阻塞把响应体数据发送到客户端，重置缓冲区，以供写入
     *
     * @throws IOException
     */
    public void flush() throws IOException {
        ByteBuffer writeBuffer = bufferHandler.getWriteBuffer();
        writeBuffer.flip();
        if (writeBuffer.remaining() > 0) {
            log.info("模拟阻塞写入 - 将响应体 [{}B] 数据写入通道 [{}]", writeBuffer.remaining(), this);
        }
        while (writeBuffer.hasRemaining()) { // TODO 超时处理
            int n = socket.write(writeBuffer);
            if (n == -1) throw new EOFException();
            if (n > 0) { // write success
                log.debug("  阻塞写入 [{}B] 字节", n);
            }
//            log.info("  写入 [{}B] 字节，完成响应任务", n);
        }
        writeBuffer.clear();
    }
}
