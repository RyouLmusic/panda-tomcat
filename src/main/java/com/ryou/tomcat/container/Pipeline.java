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
 * 容器处理流水线，本质就是一个有固定尾节点的链表
 * 一定有一个 basic的Valve阀门
 */
public class Pipeline {
    protected Container container = null;
    
    private Valve basic = null;
    private Valve first = null;
    
    public Pipeline(Container container) {
        this.container = container;
    }
    
    /** 从第一个 Valve 开始依次调用它们的 invoke 方法 */
    public void handle(Request request, Response response) throws IOException, ServletException {
        if (first != null) {
            first.invoke(request, response);
        } else {
            basic.invoke(request, response);
        }
    }
    /**
     * 设置固定尾节点
     * 设置新的固定尾节点，旧的被替代掉
     * @param newBasic 链表末尾的阀门 Valve
     */
    public void setBasic(Valve newBasic) {
        newBasic.setContainer(container);
        Valve oldBasic = basic;
        if (oldBasic == newBasic)
            return;

        Valve current = first;
        // 迭代到Pipeline管道的最后的阀门
        while (current != null) {
            if (current.getNext() == oldBasic) {
                // 替代旧的固定尾节点
                current.setNext(newBasic);
                break;
            }
            current = current.getNext();
        }
        // basic更新
        this.basic = newBasic;
    }

    /**
     * 把阀门 Valve 正序插入处理流水线中
     * 
     * @param valve 待插入阀门
     */
    public void addValve(Valve valve) {
        valve.setContainer(container);
        // 没有起始阀门
        if (first == null) {
            first = valve;
            first.setNext(basic);
        }

        else {
            Valve current = first;
            while (current != null) {
                if (current.getNext() == basic) {
                    current.setNext(valve);
                    // 加入的节点在 固定的尾节点的前一个，（basic有可能为null，但是不会影响，但是不应该出现为null的情况）
                    valve.setNext(basic);
                    break;
                }
                current = current.getNext();
            }
        }
    }
}
