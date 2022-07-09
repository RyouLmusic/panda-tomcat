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
package com.ryou.tomcat.utils.digester.rule;

import com.ryou.tomcat.utils.digester.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 设置类之间的组合关系，假设栈顶元素为 a，下一个元素为 b，则调用 b.setXX(a)，将 a 设置为 b 的成员
 * 
 *  
 */
public class SetNextRule extends Rule {
    final static Logger log = LoggerFactory.getLogger(SetNextRule.class);
    
    protected String methodName = null;
    protected String paramType = null;
    
    public SetNextRule(String methodName) {
        this(methodName, null);
    }
    
    public SetNextRule(String methodName, String paramType) {
        this.methodName = methodName;
        this.paramType = paramType;
    }


    /**
     * 调用Context里的addChild()方法，将 <servlet></servlet>标签里实例化的对象 作为子容器放入Bootstrap的context里面
     * @param namespace
     * @param name
     * @throws Exception
     */
    @Override
    public void end(String namespace, String name) throws Exception {
        Object child = digester.peek(0); // 获取<servlet></servlet>标签里面的对象
        Object parent = digester.peek(1);// Context最开始就被放入了 对象栈里面
        Object[] argsType = new Class[1]; //参数类型
        if (paramType != null) {
            argsType[0] = loadClass(paramType); // paramType = com.ryou.tomcat.container.Container
        } else {
            argsType[0] = child.getClass(); //没有的话，直接获取要添加进入方法的child的class类型
        }
        
        log.debug("[SetNextRule]{{}} Call {}.{}({})", digester.match(), parent.getClass().getName(), methodName, child);
        
        invokeMethod(parent, methodName, new Object[]{child}, argsType);
    }

    /**
     * Render a printable version of this Rule.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SetObjectFieldRule[");
        sb.append("methodName=");
        sb.append(methodName);
        sb.append(", paramType=");
        sb.append("]");
        return (sb.toString());
    }
}
