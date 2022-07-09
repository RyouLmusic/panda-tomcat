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
package com.ryou.tomcat.container.core.context;

import ch.qos.logback.classic.Logger;
import com.ryou.tomcat.bootstrap.Bootstrap;
import com.ryou.tomcat.container.Container;
import com.ryou.tomcat.container.Loader;
import com.ryou.tomcat.container.core.*;
import com.ryou.tomcat.container.servletx.AppContext;
import com.ryou.tomcat.container.servletx.FilterWrapper;
import com.ryou.tomcat.container.session.Manager;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 应用程序在内部的表现类，包含 web.xml 配置的参数、Servlet 和 Filter
 *
 */
public class Context extends Container {
    final Logger log = (Logger) LoggerFactory.getLogger(Context.class);
    
    private Connector connector;
    
    public static final String AppWebXml = "/WEB-INF/web.xml";
    public static final String  RESOURCES_ATTR = "app.resources";

    /**
     * 存放项目的文件夹名字
     */
    private String appBase = "webapp";
    public String getAppBase() {
        return appBase;
    }
    public void setAppBase(String appBase) {
        this.appBase = appBase;
    }

    /**
     * 起始页
     */
    private String welcomeFile = "index.html";
    public String getWelcomeFile() {
        return welcomeFile;
    }
    public void setWelcomeFile(String welcomeFile) {
        this.welcomeFile = welcomeFile;
    }

    /** 容器类加载器，从 cytomcat.home/lib/ 目录加载类 */
    private ClassLoader parentClassLoader = Context.class.getClassLoader();
    public void setParentClassLoader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }
    public ClassLoader getContainerClassLoader() {
        return parentClassLoader;
    }


    private AppContext appContext;
    public ServletContext getServletContext() {
        if (appContext == null) {
            // 实例化ServletContext
            appContext = new AppContext(this); // 是一个全局的储存信息的空间，服务器开始就存在，服务器关闭才释放。
        }
        return appContext;
    }

    private WebResource resources;
    
    /** 应用正在热部署 */
    private volatile boolean paused = false;
    /** 应用是否正在热部署 */
    public boolean getPaused() {
        return paused;
    }
    public void setPaused(boolean value) {
        paused = value;
    }


    /** 是否是集群 */
    private boolean distributable = false;
    
    /** web 应用的名称 如：j2ee，*/
    private String docBase;
    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }
    public String getDocBase() {
        return docBase;
    }

    /**
     *
     */
    private String docBasePath;
    /** 部署的 web 应用所在的绝对路径，比如 /opt/cytomcat/webapp/test */
    public String getDocBasePath() {
        if (docBasePath == null) {
            //appBase: webapp
            // docBase: j2ee2
            Path base = Paths.get(appBase, docBase);
            docBasePath = base.toAbsolutePath().toString();
        }
        return docBasePath;
    }
    public void setDocBasePath(String docBasePath) {
        this.docBasePath = docBasePath;
    }
    /**
     * 获取文件真实的绝对路径
     *
     * @param path 文件相对路径
     * @return 绝对路径，比如 index.html -> /opt/cytomcat/webapp/test/index.html
     */
    public String getRealPath(String path) {

        File file = new File(getDocBasePath(), path);
        return file.getAbsolutePath();
    }

    /** 默认Servlet，/* */
    private Wrapper defaultWrapper;
    public Wrapper getDefaultWrapper() {
        return defaultWrapper;
    }
    
    // web.xml 中的 Servlet & Mapping
    /** 
     * 精确匹配 - 完全匹配的 URL，比如 /catalog，URL 必须与它相等才能匹配
     */
    private TreeMap<String, Wrapper> exactWrappers = new TreeMap<>();

    public TreeMap<String, Wrapper> getExactWrappers() {
        return exactWrappers;
    }

    /** 
     * 扩展名匹配 - 以 '*.' 为前缀的 URL，比如 *.bop，存储时的 key 是 bop，去除  *.
     */
    private TreeMap<String, Wrapper> extensionWrappers = new TreeMap<>();
    public TreeMap<String, Wrapper> getExtensionWrappers() {
        return extensionWrappers;
    }
    /**
     * 从 web.xml 提取 servlet-mapping
     *
     * @param servletName 配置的 servlet 名称
     * @param urlPattern 配置的要处理的 url
     */
    public void addServletMapping(String servletName, String urlPattern) {
        Wrapper servletWrapper = (Wrapper) findChild(servletName);
        if (servletWrapper == null) {
            throw new IllegalArgumentException("unknown servlet name");
        }
        String key = null;
        if (urlPattern.endsWith("/*")) {
            key = urlPattern.substring(0, urlPattern.length() - 2);
            wildcardWrappers.put(key, servletWrapper);
        } else if (urlPattern.startsWith("*.")) {
            key = urlPattern.substring(2);
            extensionWrappers.put(key, servletWrapper);
        } else if (urlPattern.equals("/")) {
            defaultWrapper = servletWrapper;
        } else {
            key = urlPattern;
            exactWrappers.put(key, servletWrapper);
        }
    }
    /** web.xml 中的 Filter */
    private HashMap<String, FilterWrapper> filters = new HashMap<>();
    /**
     * 从 web.xml 提取 filter-mapping
     * @param filterName 配置的 filter 名称
     * @param urlPattern 配置的 url 映射，支持多个
     */
    public void addFilterMapping(String filterName, String urlPattern) {
        FilterWrapper filterWrapper = filters.get(filterName);
        if (filterWrapper == null) {
            throw new IllegalArgumentException("unknown filter name");
        }
        filterWrapper.addURLPattern(urlPattern);
    }
    public void addFilterWrapper(FilterWrapper filter) {
        filter.setContext(this);
        filters.put(filter.getFilterName(), filter);
    }
    public HashMap<String, FilterWrapper> getFilters() {
        return filters;
    }


    /**
     * 类加载器
     */
    private Loader loader;

    /**
     * 获取类加载器
     * @return URLClassLoader 扩展加载器，可以加载jar包和外部的class文件
     */
    public Loader getLoader() {
        return loader;
    }
    public void reload() throws Exception {
        log.debug("Context [{}] is reloading ...", docBase);
        setPaused(true);

        // 将成功加载的 Servlet 和 Filter 释放
        Container[] wrappers = findChildren();
        for (Container wrapper : wrappers) {
            try {
                ((Wrapper)wrapper).stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (FilterWrapper filter : filters.values()) {
            filter.release();
        }

        loader.stop();

        // 创建一个新的类加载器，这样旧的 Loader 加载的类会全部被卸载回收
        loader = new Loader(parentClassLoader, this);

        setPaused(false);
        log.debug(" Context [{}] reload is completed", docBase);
    }


    /**
     * session管理器
     */
    private Manager manager;
    public Manager getManager() {
        return manager;
    }


    /** context-param 配置的参数 */
    private final ConcurrentMap<String, String> parameters = new ConcurrentHashMap<>();
    public String getParameter(String name) {
        return parameters.get(name);
    }


    /**
     * 存储请求的文件格式
     */
    private final HashMap<String, String> mimeMappings = new HashMap<>();
    public String findMimeMapping(String extension) {
        return mimeMappings.get(extension);
    }


    /**
     * 模糊匹配 以 - '/*' 结尾的 URL，比如 /foo/*，存储时的 key 是 /foo，去除 /*
     */
    private final TreeMap<String, Wrapper> wildcardWrappers = new TreeMap<>();
    public TreeMap<String, Wrapper> getWildcardWrappers() {
        return wildcardWrappers;
    }


    /**
     * 为属于自己的管道添加尾节点
     * 添加一个声明周期监听者
     */
    public Context() {
        // 为管道设置一个固定的尾节点
        pipeline.setBasic(new ContextBasicValve());
        // 添加一个声明周期监听者，处理部署
        addLifecycleListener(new ContextConfig());
    }

    /**
     * 此方法没有被调用，是在{@link Bootstrap} 使用反射的方式对此方法进行 了调用
     * @throws Exception
     */
    @Override
    public void init() throws Exception {

        // 实例化默认的servlet容器，可以处理 '/' 的请求路径
        defaultWrapper = new Wrapper();
        defaultWrapper.setName("default");
        defaultWrapper.setServletClass("com.ryou.tomcat.container.servletx.DefaultServlet");
        // 将此容器 添加到 this的容器的 子容器集合里面
        addChild(defaultWrapper);
        // 静态资源
        resources = new WebResource(this);
        // 往ServletContext里面添加属性 TODO ??
        getServletContext().setAttribute(RESOURCES_ATTR, resources);


        // 存储请求的文件格式
        mimeMappings.put("css","text/css");
        mimeMappings.put("exe","application/octet-stream");
        mimeMappings.put("gif","image/gif");
        mimeMappings.put("htm","text/html");
        mimeMappings.put("html","text/html");
        mimeMappings.put("ico","image/x-icon");
        mimeMappings.put("jpe","image/jpeg");
        mimeMappings.put("jpeg","image/jpeg");
        mimeMappings.put("jpg","image/jpeg");
        mimeMappings.put("js","application/javascript");
        mimeMappings.put("json","application/json");
        mimeMappings.put("png","image/png");
        mimeMappings.put("svg","image/svg+xml");
        mimeMappings.put("txt","text/plain");
        mimeMappings.put("xml","application/xml");

        /** 引起观察者 运行观察函数*/
        /** 初始化事件， 会使{@link ContextConfig}ContextConfig.lifecycleEvent()运行*/
        fireLifecycleEvent(LifecycleEventType.INIT);

    }


    /**
     * 后台守护线程
     * @throws Exception
     */
    @Override
    public void backgroundProcess() throws Exception {

        if (loader != null) {
            // 检查类是否变动，是否要重新加载，这里实现的就是所谓的热加载
            loader.backgroundProcess();
        }

        if (manager != null) {
            // 周期性查找 session是否失效
            manager.backgroundProcess();
        }
    }

    @Override
    public void startInternal() throws Exception {

        // 添加一个用于报告错误的 Valve
        pipeline.addValve(new ErrorReportValve());

        // Session 管理，Cluster TODO
        manager = new Manager();

        // 初始化 web 应用类加载器
        loader = new Loader(parentClassLoader, this);

        // 初始化并启动连接器
        connector = new Connector();
        connector.setContext(this);
        connector.start();
    }

    @Override
    public void stop() throws Exception {

        connector.stop();

        // destory filters
        for (FilterWrapper filterWrapper : filters.values()) {
            filterWrapper.release();
        }
        filters.clear();
    }

    @Override
    public void log(String msg) {
        log.info(msg);
    }

}
