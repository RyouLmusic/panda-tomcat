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

import com.ryou.tomcat.bootstrap.Bootstrap;

import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 生命周期抽象类，为组件的初始化、启动和停止提供统一的机制
 *
 * 抽象目标：被观察者们观察的抽象目标
 * 具体目标为：Container、Context、Wrapper 这些容器
 *
 */
public abstract class Lifecycle {
    
    /** 观察者模式，用于通知事件而注册的监听器列表 */
    private final List<LifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();
    
    /** 启动前的初始化工作， 初始完毕会触发 INIT 事件, 在{@link Bootstrap}被反射调用*/
    public abstract void init() throws Exception;
    
    /** 启动组件， 并触发 START 事件*/
    public abstract void start() throws Exception;
    
    /** 停止组件， 并触发 STOP 事件*/
    public abstract void stop() throws Exception;
    /** 记录日志*/
    public abstract void log(String msg);


    /** 添加一个观察者 */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycleListeners.add(listener);
    }
    /** 移除一个观察者 */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }
    /**
     * 触发一个声明周期事件，，通知观测者
     * @param type
     * @throws Exception 
     */
    protected void fireLifecycleEvent(LifecycleEventType type) throws Exception {
        fireLifecycleEvent(type, null);
    }
    protected void fireLifecycleEvent(LifecycleEventType type, Object data) throws Exception {
        // 发生的事件
        LifecycleEvent event = new LifecycleEvent(this, type, data);
        // 调用观察者的 响应方法
        for (LifecycleListener listener : lifecycleListeners) {
            listener.lifecycleEvent(event);
        }
    }


    /** 事件类型 */
    public static enum LifecycleEventType {
        /** 初始化事件 */
        INIT,
        /** 组件启动事件 */
        START,
        /** 组件停止事件 */
        STOP
    }
    /** 生命周期监听器接口 */
    /**
     * 抽象观察者
     * 具体观察者 : ContextConfig
     */
    public static interface LifecycleListener {
        /** 处理对应的生命周期事件 */
        /**
         * 观察者在 观察到 目标发生了变动的时候 调用的响应方法
         * @param event
         * @throws Exception
         */
        public void lifecycleEvent(LifecycleEvent event) throws Exception;
    }



    /** 生命周期事件封装的对象 */
    public static final class LifecycleEvent extends EventObject {
        private static final long serialVersionUID = 1L;
        private final Object data;
        private final LifecycleEventType type;
        
        public LifecycleEvent(Lifecycle lifecycle, LifecycleEventType type, Object data) {
            super(lifecycle); // getSource() 里面的返回的resource就是在此被赋值为 lifecycle的
            this.type = type;
            this.data = data;
        }

        public Object getData() {
            return data;
        }
        public LifecycleEventType getType() {
            return type;
        }
        public Lifecycle getLifecycle() {
            return (Lifecycle) getSource(); // 所以此处的返回的Lifecycle
        }
    }
}
