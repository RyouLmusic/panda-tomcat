package com.ryou.tomcat.http;

import java.nio.ByteBuffer;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 16:23
 *
 * 在读取请求数据时，所有的缓冲区都是底层 NioChannel 中 ByteBuffer
 * 的视图，为了更方便的获取这个视图，提供这样的一个回调接口
 */
public interface BufferHolder {

    /**
     * 设置读取结果数据的视图 ByteBuffer
     *
     * @param buffer
     */
    void setByteBuffer(ByteBuffer buffer);

    /**
     * 获取读取数据的视图 ByteBuffer
     */
    ByteBuffer getByteBuffer();
}
