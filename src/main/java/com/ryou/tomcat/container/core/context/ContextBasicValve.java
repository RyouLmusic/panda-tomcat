package com.ryou.tomcat.container.core.context;

import com.ryou.tomcat.container.Valve;
import com.ryou.tomcat.container.core.Wrapper;
import com.ryou.tomcat.container.core.context.Context;
import com.ryou.tomcat.container.servletx.Request;
import com.ryou.tomcat.container.servletx.Response;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Context 处理管道 的 固定尾阀门
 */
public class ContextBasicValve extends Valve {

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        String requestPath = request.getRequestURI(); // j2ee/hello
        if ((requestPath.startsWith("/META-INF/", 0))
                || (requestPath.startsWith("/META-INF"))
                || (requestPath.startsWith("/WEB-INF/", 0))
                || (requestPath.equalsIgnoreCase("/WEB-INF"))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // 获取 此项目的顶级  容器
        Context cxt = request.getContext();

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        // 将当前线程的上下文类加载器替换成 Web 应用类加载器  (这个切换类加载器的动作 Tomcat 是在 StandardHostValve 中做的)
        Thread.currentThread().setContextClassLoader(cxt.getLoader().getClassLoader());
        // 获取此次  请求的wrapper容器
        Wrapper wrapper = request.getWrapper();
        // 开始处理 此次请求的wrapper容器的 通道 阀门了
        wrapper.getPipeline().handle(request, response);

        // 还原当前线程的上下文类加载器
        Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
}
