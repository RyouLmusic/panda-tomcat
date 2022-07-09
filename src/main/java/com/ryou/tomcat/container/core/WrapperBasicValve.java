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
import com.ryou.tomcat.container.Valve;
import com.ryou.tomcat.container.servletx.AppFilterChain;
import com.ryou.tomcat.container.servletx.Request;
import com.ryou.tomcat.container.servletx.Response;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Wrapper 处理通道固定尾节点，加载 Servlet，创建 FilterChain，调用 Servlet.service 方法
 *
 * 是处于最后 的一个 阀门
 */
public class WrapperBasicValve extends Valve {
    final static Logger log = (Logger) LoggerFactory.getLogger(WrapperBasicValve.class);

    /**
     * 责任链模式的 执行方法
     * @param request 要处理的请求对象
     * @param response 要处理的响应对象
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        Wrapper wrapper = request.getWrapper();

        Servlet servlet = null;

        try {
            // 实例化 servlet对象
            servlet = wrapper.allocate();
        } catch (Throwable t) {
            log.error("Allocate exception for servlet [{" + wrapper.getName() + "}]", t);
            request.setAttribute("javax.servlet.exception", t);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        AppFilterChain filterChain = AppFilterChain.createFilterChain(request, wrapper, servlet);

        if (servlet != null && filterChain != null) {
            try {
                // 调用所有filter的doFilter方法
                // 最后执行servlet.service()方法
                filterChain.doFilter(request, response);
            } catch (Throwable t) {
                log.error("Servlet.service() for servlet [{" + wrapper.getName() + "}] throw exception",t);
                request.setAttribute("javax.servlet.exception", t);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } finally {
                filterChain.release();
            }
        }
    }
}
