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
import com.ryou.tomcat.container.Container;
import com.ryou.tomcat.container.Loader;
import com.ryou.tomcat.container.core.context.Context;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Enumeration;

/**
 * 与 Servlet 一一对应，管理实现 Servlet 生命周期方法，加载、初始化和销毁
 */
public class Wrapper extends Container implements ServletConfig {
    final static Logger log = (Logger) LoggerFactory.getLogger(Wrapper.class);

    private volatile Servlet instance;
    /**
     * @return Servlet对象实例
     */
    public Servlet getInstance() {
        return instance;
    }
    public void setInstance(Servlet instance) {
        this.instance = instance;
    }

    // Servlet对象的  包名全称
    private String servletClass;
    public String getServletClass() {
        return servletClass;
    }
    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;
    }

    /**
     * 已经去除 webapp项目名称 的   请求路径   如  /hello、/login
     */
    private String wrapperPath;
    /**
     * /hello 、 /login 等请求路径，并且与wrapper容器相匹配
     * @return
     */
    public String getWrapperPath() {
        return wrapperPath;
    }
    public void setWrapperPath(String wrapperPath) {
        this.wrapperPath = wrapperPath;
    }


    /**
     * 每个wrapper对象实例化的时候 都会添加 固定的尾阀门
     */
    public Wrapper() {
        pipeline.setBasic(new WrapperBasicValve());
    }

    @Override
    public void addChild(Container child) {
        throw new IllegalStateException("Wrapper container may not have child containers");
    }

    /**
     * 将从web.xml 获取的servletClass -->实例化为真实的对象， 可以调用其service方法
     * @return
     * @throws ServletException
     */
    public Servlet allocate() throws ServletException {
        // 防止多次实例化
        if (instance == null) {
            instance = loadServlet();
        }
        return instance;
    }

    /**
     * 实例化servletClass的过程
     * @return Servlet对象实例，跟请求对应---此对应关系来自 web.xml
     * @throws ServletException
     */
    public Servlet loadServlet() throws ServletException {
        Servlet servlet = null;
        Class<?> clazz = null;

        // Bootstrap里面的context
        Context appCxt = (Context) getParent();
        // 获取
        Loader loader = appCxt.getLoader();
        ClassLoader cl = loader.getClassLoader();

        try {
            // 自定义的类加载器不为空，可以用其加载
            if (cl != null) {
                clazz = cl.loadClass(servletClass);
            } else {
                clazz = Class.forName(servletClass);
            }
            // 获取Servlet对象
            servlet = (Servlet) clazz.newInstance();
            // 调用servlet对象的init方法
            servlet.init(this);
        } catch (Exception e) {
            log.error("Error loading " + cl + " " + servletClass, e);
            throw new ServletException(e.getMessage());
        }
        return servlet;
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public void startInternal() {
    }

    @Override
    public void stop() throws Exception {
        if (instance != null) {
            log.debug("  Destroy servlet [{}]", servletClass);
            instance.destroy();
            instance = null;
        }
    }


    /**
     * 热部署
     */
    @Override
    public void backgroundProcess() {
        // Servlet 或者 jsp 重新加载
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("@").append(servletClass);
        return sb.toString();
    }

    // ServletConfig Methods
    @Override
    public String getServletName() {
        return name;
    }

    @Override
    public ServletContext getServletContext() {
        return ((Context)getParent()).getServletContext();
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return null;
    }
    @Override
    public void log(String msg) {
        log.info(msg);
    }

}
