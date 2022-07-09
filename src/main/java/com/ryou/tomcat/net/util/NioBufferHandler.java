package com.ryou.tomcat.net.util;

import java.nio.ByteBuffer;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/24 12:13
 *
 * NIO的读写buffer
 */
public class NioBufferHandler {

    private final ByteBuffer readBuf;
    private final ByteBuffer writeBuf;

    /**
     * 进行读写buffer的初始化
     * @param readSize 读buffer的大小
     * @param writeSize 写操作buffer的大小
     */
    public NioBufferHandler(int readSize, int writeSize) {
        readBuf = ByteBuffer.allocateDirect(readSize);
        writeBuf = ByteBuffer.allocateDirect(writeSize);
    }

    public ByteBuffer expand(ByteBuffer buffer, int remaining) {return buffer;}
    public ByteBuffer getReadBuffer() {return readBuf;}
    public ByteBuffer getWriteBuffer() {return writeBuf;}
}
