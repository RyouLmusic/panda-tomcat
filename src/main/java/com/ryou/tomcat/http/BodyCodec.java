package com.ryou.tomcat.http;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 16:02
 *
 * 请求体解码器、响应体编码器
 */
public interface BodyCodec {

    int maxSwallowSize = 1024 * 1024; // 1MB

    /**
     * 读取 POST 请求体数据，实现了 chunked 和 identity 两种传输方式
     *
     * @param input 关联的 HTTP 请求解析类
     * @return -1 表示读取完毕，>=0 表示读到了数据
     * @throws IOException
     */
    int doRead(InputBuffer input) throws IOException;

    /**
     * 如果服务端准备发送异常响应，但是请求体还有数据未读（比如当上传一个过大的文件时，服务端
     * 发现超过限制，但客户端仍在发送数据） 这个时候，为了让客户端能够接收到响应，服务端应该继
     * 续纯读取剩余的请求体数据，如果超过 maxSwallowSize 抛异常关闭连接
     *
     * @param input 关联的 HTTP 请求解析类
     * @throws IOException 发生 IO 异常，关闭连接
     */
    void endRead(InputBuffer input) throws IOException;

    /**
     * 将响应体数据写入缓冲区，chunked 和 identity 写入方式不一样
     *
     * @param output 关联的响应编码处理类
     * @param src 待写入的数据
     * @throws IOException
     */
    void doWrite(OutputBuffer output, ByteBuffer src) throws IOException;

    void endWrite(OutputBuffer output) throws IOException;
}
