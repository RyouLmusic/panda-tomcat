package com.ryou.tomcat.bootstrap;

import com.ryou.tomcat.container.core.context.Context;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/7 12:05
 *
 * 项目启动类
 */
public class Bootstrap {

    /**
     * 初始用到的上下文容器
     */
    private Context context;
    /**
     * 类加载器
     */
    private ClassLoader classLoader;

    /**
     * 启动方法
     */
    private void start() throws Exception {
        long start = System.nanoTime();
        init();
        // 启动容器
        context.start();
        // 记录日志
        StringBuilder sb = new StringBuilder();
        sb.append("Server startup in ").append((System.nanoTime() - start) / 1000000).append(" ms");
        context.log(new String(sb));

        // 钩子线程：jvm关闭的时候会 运行此线程
        Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {

            @Override
            public void run() {
                try {
                    Bootstrap.this.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void stop() throws Exception {
        context.stop();
    }

    /**
     * 初始化方法，调用各个容器类的初始化方法
     */
    private void init() throws Exception {

        classLoader = createClassLoader();
        // 设置当前线程上下文类加载器为 commonLoader，后续的资源或者 class 基本上都在 ${cytomcat.base}/lib 下
        Thread.currentThread().setContextClassLoader(classLoader);

        context = new Context();
        context.setParentClassLoader(classLoader);
        // 初始化容器
        context.init();
    }

    /**
     * 类加载器
     * @return 返回一个带加载配置文件的 类加载器，还可以加载jar包和class文件
     * @throws MalformedURLException
     */
    private ClassLoader createClassLoader() throws MalformedURLException {


        return new URLClassLoader(new URL[]{});

    }


    public static void main(String[] args) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.start();
    }
}
