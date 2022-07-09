package com.ryou.tomcat.http.codec;

import com.ryou.tomcat.http.BodyCodec;
import com.ryou.tomcat.http.InputBuffer;
import com.ryou.tomcat.http.OutputBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 17:06
 *
 *
 * 请求体的解析 要根据Content-type来进行
 * multipart/form-data
 * application/x-www-form-urlencoded
 * text/plain
 * application/json
 *
 * binary：二进制文件
 */
public class ChunkedCodec extends BodyDecodeContext implements BodyCodec {

    private String contentType;

    public ChunkedCodec(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public int doRead(InputBuffer input) throws IOException {

        BufferedReader bufferedReader = input.getBufferedReader();

        bufferedReader.lines().forEach(System.out::println);

        return 0;
    }

    @Override
    public void endRead(InputBuffer input) throws IOException {

    }

    @Override
    public void doWrite(OutputBuffer output, ByteBuffer src) throws IOException {

    }

    @Override
    public void endWrite(OutputBuffer output) throws IOException {

    }
}
