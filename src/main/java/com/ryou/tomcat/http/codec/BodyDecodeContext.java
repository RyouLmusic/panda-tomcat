package com.ryou.tomcat.http.codec;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/27 13:39
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
public abstract class BodyDecodeContext  {

    private ParsesRequestBody parsesRequestBody;

    /**
     * 获取解析策略
     * @return
     */
    public ParsesRequestBody getStrategy() {
        return parsesRequestBody;
    }

    /**
     * 设置解析策略
     * @param parsesRequestBody
     */
    public void setStrategy(ParsesRequestBody parsesRequestBody) {
        this.parsesRequestBody = parsesRequestBody;
    }

    /**
     * 调用解析策略的  解析方法
     * @param bufferedReader BufferedReader：包含请求体的内容
     */
    public String decodeMethod(BufferedReader bufferedReader, int contentLength) throws IOException {
        return parsesRequestBody.decodeMethod(bufferedReader, contentLength);
    }
}
