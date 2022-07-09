package com.ryou.tomcat.http.codec;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/27 13:47
 */
public interface ParsesRequestBody {

    String decodeMethod(BufferedReader bufferedReader, int contentLength) throws IOException;
}
