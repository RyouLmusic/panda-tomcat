package com.ryou.tomcat.http.codec.identityBody;

import com.ryou.tomcat.http.codec.ParsesRequestBody;

import java.io.BufferedReader;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/27 14:46
 * Content-Type:application/json 格式的解析
 *
 * Content-Type:application/json
 *
 * {
 *     "abc": "sdh",
 *     "hty": "adc"
 * }
 */
public class IdentityApplicationJson implements ParsesRequestBody {

    @Override
    public String decodeMethod(BufferedReader bufferedReader, int contentLength) {


        bufferedReader.lines().forEach(v -> {
        });
        return "";
    }
}
