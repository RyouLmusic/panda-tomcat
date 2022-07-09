/**
 * Copyright 2019 chuonye.com - 小创编程
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ryou.tomcat.container.core;


import ch.qos.logback.classic.Logger;
import com.ryou.tomcat.container.core.context.Context;
import com.ryou.tomcat.container.servletx.Request;
import com.ryou.tomcat.container.servletx.Response;
import com.ryou.tomcat.http.Adapter;
import com.ryou.tomcat.http.RawRequest;
import com.ryou.tomcat.http.RawResponse;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.TreeMap;

/**
 * 适配容器，主要是映射 Servlet，尝试从 Cookie 中解析 Session ID
 *
 */
public class AdapterImpl implements Adapter {

    final Logger log = (Logger) LoggerFactory.getLogger(AdapterImpl.class);

    private Connector connector;

    public AdapterImpl(Connector connector) {
        this.connector = connector;
    }

    @Override
    public void service(RawRequest rawReq, RawResponse rawResp) throws Exception {
        // 实例化容器中的 request
        Request httpServletRequest = new Request();
        httpServletRequest.setRawReq(rawReq);
        // 实例化容器中的响应 response
        Response httpServletResponse = new Response();
        httpServletResponse.setRawResp(rawResp);

        // 将两个容器中的 request和response进行关联
        httpServletRequest.setResponse(httpServletResponse);
        httpServletResponse.setRequest(httpServletRequest);

        // 进入容器生成响应
        try {
            if (postParseRequest(rawReq, httpServletRequest, rawResp, httpServletResponse)) {
                // 此容器是（Bootstrap里面实例化的context）项目的顶级容器  通过容器获取管道，调用管道里面的所有阀门的处理方法，进行处理 请求，生成响应
                connector.getContainer().getPipeline().handle(httpServletRequest, httpServletResponse);
            }
            httpServletResponse.finish();
        } finally {
            // 清理
            httpServletRequest.recycle();
            httpServletResponse.recycle();
        }

    }

    /**
     * 请求的uri进行解析和规范化
     * 寻找与此请求路径相匹配的wrapper容器
     * 解决cookies和session的内容以后
     *
     * @param rawReq 自定义的request
     * @param request HttpServletRequest
     * @param rawResp 自定义的response
     * @param response HttpServletResponse
     * @return 是否加载成功
     */
    private boolean postParseRequest(RawRequest rawReq, Request request, RawResponse rawResp, Response response) {

        //
        Context context = connector.getContainer();
        // 对uri进行规范化
        String uri = rawReq.getUri();
        try {
            uri = URLDecoder.decode(uri, rawReq.getEncoding().name());
        } catch (UnsupportedEncodingException e) {
        }

        // uri 最前面是'/'接下来就是  j2ee 项目的名字，如果项目的名字匹配，说明此context就是此请求的
        // 匹配 Context
        if (uri.startsWith(context.getDocBase(), 1)) {
            // 将匹配到的容器(context) 放入请求里面
            request.setContext(context);
        } else {
            log.debug("匹配 Web 上下文对象 Context 失败，响应 404");
            rawResp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }

        // uri 去除应用名称
        uri = uri.substring(uri.indexOf(context.getDocBase()) + context.getDocBase().length());
        // 没有 Servlet Path
        if ("".equals(uri)) {
            uri += "/";
        }

        // 开始进行映射请求 和 容器(wrapper)
        boolean mapRequired = true;
        while (mapRequired) {
            // 通过context容器和请求路径 进行创建 wrapper容器
            Wrapper wrapper = mapServlet(context, uri);
            // 将wrapper容器放入request
            request.setWrapper(wrapper);

            // Parse session id in Cookie TODO cookies和session的内容以后
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("JSESSIONID".equalsIgnoreCase(cookie.getName())) {
                        String reqId = cookie.getValue();
                        request.setSessionId(reqId);
                    }
                }
            }


            StringBuilder sb = new StringBuilder(120);
            sb.append("映射 Servlet\r\n======Mapping Result======");
            sb.append("\r\n  Request Path: ").append(uri);
            sb.append("\r\n  Context: /").append(context.getDocBase());
            sb.append("\r\n  Wrapper: ").append(wrapper);
            sb.append("\r\n  jsessionid: ").append(request.getRequestedSessionId());
            sb.append("\r\n==========================");
            log.info(sb.toString());

            mapRequired = false;

            // Tomcat 在这里进行了多版本 Context 检测，由并行部署同一个 Web 应用导致不同的版本
            // 简单起见，这里只检测应用 class 文件是否正在热加载，没有实现 web.xml 检测和部署
            if (context.getPaused()) {
                log.debug("Web 应用 [/{}] 正在热加载，重新映射 Servlet", context.getDocBase());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Should never happen
                }
                // reset mapping
                request.recycle();
                wrapper = null;
                mapRequired = true;
            }
        }
        return true;
    }

    /**
     * 映射 Servlet
     *
     * @param context 请求匹配的应用上下文对象
     * @param uri 请求 Servlet 路径
     * @return 返回的肯定不为空，默认返回 DefaultServlet
     */
    private Wrapper mapServlet(Context context, String uri) {

        Wrapper mapWrapper = null;
        // Rule 1 -- Exact Match 精确匹配 /catalog
        TreeMap<String, Wrapper> exactWrappers = context.getExactWrappers(); // web.xml中的servlet和url映射关系
        // 获取exactWrappers的key比uri小或者相等的 中 最大的key
        String key = exactWrappers.floorKey(uri);
        // 如果可以获取到相等的key，就说明此uri已经有与之匹配的 servlet容器了
        if (uri.equals(key)) {
            // 直接获取容器
            mapWrapper = exactWrappers.get(key);
        }
        // Rule 2 -- Prefix Match 模糊匹配 /foo/bar/*
        if (mapWrapper == null) {
            TreeMap<String, Wrapper> wildcardWrappers = context.getWildcardWrappers();
            key = wildcardWrappers.floorKey(uri);
            if (key != null) {
                // uri = /foo/bar, a/foo/bar, a/foo/bar/c, a/foo/bar/c/d
                // name = /foo/bar
                if (uri.startsWith(key) || uri.endsWith(key) || uri.contains(key + "/")) {
                    mapWrapper = wildcardWrappers.get(key);
                }
            }

        }

        // 以 '*.' 为前缀的 URL，比如 *.bop，存储时的 key 是 bop，去除  *.
        // Rule 3 -- Extension Match 扩展名，最长路径的模糊匹配
        if (mapWrapper == null) {
            TreeMap<String, Wrapper> extensionWrappers = context.getExtensionWrappers();
            // key是后缀
            key = extensionWrappers.floorKey(uri);
            // 如果uri的后缀是此key，说明此wrapper是与此uri映射的
            if (key != null && uri.endsWith("." + key)) {
                mapWrapper = extensionWrappers.get(key);
            }
        }

        // Rule 4 -- Welcome resources processing for servlets
        // 如果uri是只有从‘/’,直接将uri设置为 欢迎页的路径
        if (mapWrapper == null) {
            if (uri.endsWith("/")) {
                uri += context.getWelcomeFile();
            }
        }

        // Rule 5 -- Default servlet
        // 前面的都没有匹配到 uri的wrapper容器，就设置为默认的servlet容器
        if (mapWrapper == null) {
            mapWrapper = context.getDefaultWrapper();
        }

        // 将此次的请求uri交由与之匹配的wrapper进行处理
        mapWrapper.setWrapperPath(uri);

        return mapWrapper;
    }
}
