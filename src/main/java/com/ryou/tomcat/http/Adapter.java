package com.ryou.tomcat.http;

import java.io.IOException;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 16:01
 *
 * 容器适配器，用于连接 Endpoint 和 Container
 */
public interface Adapter {

    /**
     * 处理请求，生成响应
     *
     * @param request 底层原始请求对象
     * @param response 底层原始响应对象
     * @throws IOException
     */
    void service(RawRequest request, RawResponse response)  throws Exception;
}
