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
package com.ryou.tomcat.container;

import com.ryou.tomcat.container.servletx.Request;
import com.ryou.tomcat.container.servletx.Response;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * 阀门抽象类，这个类取义于现实生活中的管道阀门的概念。所有的阀门都有机会处理请求和响应
 *
 * 责任链模式：
 * Valve是抽象的处理角色(可以设置下一个处理角色、得到下一个处理角色，最主要抽象了每个处理角色自己的处理方法)
 * 具体的处理角色：ContextBasicValve、ErrorReportValve、WrapperBasicValve
 * 来自客户端的请求会经过Pipeline管道，然后执行这些处理角色的处理函数，通过对应的责任进行处理
 */
public abstract class Valve {
    protected Container container = null;
    
    private Valve next = null;
    /** 设置下一个 Valve */
    public void setNext(Valve valve) {
        next = valve;
    }

    /** 获取下一个 Valve */
    public Valve getNext() {
        return next;
    }
    
    /**
     * 检查或者修改 Request 和 Response 的属性，如果没有生成响应使用 getNext().invoke 调用管道中的下一个 Valve
     *
     * 抽象的处理方法
     * @param request 要处理的请求对象
     * @param response 要处理的响应对象
     */
    public abstract void invoke(Request request, Response response) throws IOException, ServletException;
    
    public void setContainer(Container container) {
        this.container = container;
    }
}
