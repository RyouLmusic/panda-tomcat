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
import com.ryou.tomcat.container.Lifecycle;
import com.ryou.tomcat.container.core.context.Context;
import com.ryou.tomcat.utils.digester.Digester;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;

/**
 * 容器 Context 生命周期监听类，用于配置和启动
 */
public class ContextConfig implements Lifecycle.LifecycleListener {
    final static Logger log = (Logger) LoggerFactory.getLogger(ContextConfig.class);
    
    /** 管理的 Context */
    private Context context;
    private boolean deployed = false;
    
    /** 用于解析 web.xml 的 Digester */
    private Digester webXmlParser;

    /**
     * 观察者模式的 观察函数
     * @param event
     * @throws Exception
     */
    @Override
    public void lifecycleEvent(Lifecycle.LifecycleEvent event) throws Exception {

        context = (Context) event.getLifecycle();
        switch (event.getType()) {
            case INIT:
                init();
                break;
            case START:
                deployApp();
                break;
            case STOP:
                stop();
                break;
        }
    }

    /**
     * web.xml文件的解析做的准备
     * 准备一些规则，根据这些规则进行 解析web.xml文件
     */
    private void init() {

        webXmlParser = new Digester();
        // 设置 类加载器
        webXmlParser.setClassLoader(context.getContainerClassLoader());

        // <context-param>上下文参数
        // 声明应用范围内的初始化参数。用于向Servlet+Context提供键值对，即应用程序上下文信息。
        webXmlParser.addCallMethod("web-app/context-param", "", 2);
        webXmlParser.addCallParam("web-app/context-param/param-name", 0);
        webXmlParser.addCallParam("web-app/context-param/param-value", 1);

        //
        webXmlParser.addCallMethod("web-app/distributable", "setDistributable", 0, new Class[]{Boolean.TYPE});


        // <filter>过滤器
        /**
         * <filter>
         *     <filter-name>setCharacterEncoding</filter-name>
         *     <filter-class>com.myTest.setCharacterEncodingFilter</filter-class>
         *     <init-param>
         *         <param-name>encoding</param-name>
         *         <param-value>UTF-8</param-value>
         *     </init-param>
         * </filter>
         * <filter-mapping>
         *     <filter-name>setCharacterEncoding</filter-name>
         *     <url-pattern>/*</url-pattern>
         * </filter-mapping>
         */
        webXmlParser.addObjectCreate("web-app/filter", "com.ryou.tomcat.container.servletx.FilterWrapper");
        webXmlParser.addCallMethod("web-app/filter/filter-class", "setFilterClass", 0);
        webXmlParser.addCallMethod("web-app/filter/filter-name", "setFilterName", 0);
        webXmlParser.addSetNext("web-app/filter","addFilterWrapper");

        webXmlParser.addCallMethodMultiRule("web-app/filter-mapping","addFilterMapping", 2, 1);
        webXmlParser.addCallParam("web-app/filter-mapping/filter-name",0);
        webXmlParser.addCallParamMultiRule("web-app/filter-mapping/url-pattern", 1);


        // <servlet>
        // 遇到web-app/servlet--->创建一个servlet容器
        webXmlParser.addObjectCreate("web-app/servlet","com.ryou.tomcat.container.core.Wrapper");
        // <servlet><servlet-class/></servlet>   web-app/servlet/servlet-class上面的 容器对象可以 通过栈 被这里的标签规则使用
        webXmlParser.addCallMethod("web-app/servlet/servlet-class", "setServletClass", 0);
        webXmlParser.addCallMethod("web-app/servlet/servlet-name", "setName", 0);
        // 将一个 servlet对象 作为 Bootstrap里实例化的Context的子容器
        webXmlParser.addSetNext("web-app/servlet", "addChild", "com.ryou.tomcat.container.Container");

        webXmlParser.addCallMethodMultiRule("web-app/servlet-mapping","addServletMapping", 2, 1);
        webXmlParser.addCallParam("web-app/servlet-mapping/servlet-name", 0);
        webXmlParser.addCallParamMultiRule("web-app/servlet-mapping/url-pattern", 1);

        // TODO 添加阀门Valve的规则
    }

    /**
     * 部署过程中，目前不会直接加载 Servlet 或者 Filter
     * TODO 改进：能够部署多个 web 应用
     */
    private void deployApp() throws Exception {
        File appBase = new File(System.getProperty("panda-server.base"), context.getAppBase());
        File[] apps = appBase.listFiles();
        if (apps == null || apps.length == 0) {
            throw new IllegalArgumentException("必须在[" + System.getProperty("panda-server.base") + "]部署且只能部署 一个 web 应用才能启动");
        }

        if (apps.length > 1) {
            throw new IllegalArgumentException("只支持部署一个 web 应用");
        }

        if (!deployed) {
            // 加载webapp 文件夹 下面的 文件
            File docBase = apps[0];
            // 文件的名称即是 web应用的名称
            context.setDocBase(docBase.getName());
            context.setDocBasePath(docBase.getAbsolutePath());

            // 解析web.xml文件
            File webXml = new File(docBase, Context.AppWebXml);
            InputSource in = new InputSource(new FileInputStream(webXml));
            webXmlParser.push(context);
            webXmlParser.parse(in);
            deployed = true;

            log.info("部署 Web 应用 [/{}]", context.getDocBase());
        }
    }
    private void stop() {
    }
}