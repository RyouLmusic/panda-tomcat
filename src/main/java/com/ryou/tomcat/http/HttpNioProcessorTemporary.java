package com.ryou.tomcat.http;

import com.ryou.tomcat.net.NioChannel;
import com.ryou.tomcat.net.Processor;
import com.ryou.tomcat.net.util.SocketState;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/12 20:48
 *
 *
 * 在此类中完成：
 * 1. 得到Http请求的内容，分析http请求 获得 RedRequest
 * 2. 根据请求 交给容器处理请求并生成   响应
 */
public class HttpNioProcessorTemporary implements Processor, ActionHook {
    @Override
    public SocketState process(NioChannel socket) {

        if (!socket.isOpen()) return SocketState.CLOSED;

        ByteBuffer readBuf = socket.readBuf();
        try {
            socket.read(readBuf);
            readBuf.flip();


            Charset charset = StandardCharsets.UTF_8;
            CharBuffer charBuffer = charset.decode(readBuf);
            String s = charBuffer.toString();
            System.out.println(s);


        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteBuffer writeBuf = socket.writeBuf();

        String body = "<html><h1>Hello World!</h1></html>";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +

                // 使用Content-Length:就会造成jmeter出现一半的请求无法成功
                "\r\n" + body;

        String res = "HTTP/1.1 200 OK\r\n" +
                "Date: Mon, 23 May 2005 22:38:34 GMT\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Encoding: UTF-8\r\n" +
//                "Content-Length: 138\r\n" +
                "Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\r\n" +
                "Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)\r\n" +
                "ETag: \"3f80f-1b6-3e1cb03b\"\r\n" +
                "Accept-Ranges: bytes\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>An Example Page</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "  Hello World, this is a very simple HTML document.\n" +
                "</body>\n" +
                "</html>\n";


        writeBuf.put(res.getBytes(StandardCharsets.UTF_8));
//        writeBuf.flip();
        try {
//            socket.write(writeBuf);
            socket.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return SocketState.CLOSED;
    }

    @Override
    public void action(ActionCode actionCode, Object... param) {

    }
/*
    String body = "<html><h1>Hello World!</h1></html>";
    String response = "HTTP/1.1 200 OK\r\n" +
//                "Content-Type: application/json\r\n" +
            "Server: nginx\r\n" +
            "Date: Mon,28 oct 2019 09:21:28 GMT\r\n" +
            "Content-Type: application/json; charset=UTF-8\r\n" +
//                "Transfer-Encoding: chunked\r\n" +
            "Connection: keep-alive\r\n" +
            "Expires: 0\r\n" +
            "\r\n" + body;*/
}
