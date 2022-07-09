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
import org.xml.sax.Attributes;

import java.util.Arrays;

/**
 * 对栈顶对象调用某个方法，可配置方法参数个数以及参数类型，默认参数个数为 1，参数的值使用元素节点的文本内容
 *
 */
public class CallMethodRule extends Rule {

    /**
     * 方法名称
     */
    protected String methodName;
    /**
     * 方法参数数量
     */
    protected int paramCount;
    /**
     * 方法参数类型
     */
    protected Class<?>[] paramTypes;
    /**
     * web.xml的标签节点的 内容，----- 方法的名称和参数来自于此
     */
    protected String bodyText = null;

    /**
     * 没有参数类型， 即是String类型
     * @param methodName
     * @param paramCount
     */
    public CallMethodRule(String methodName, int paramCount) {
        this(methodName, paramCount, null);
    }

    public CallMethodRule(String methodName, int paramCount, Class<?>[] paramTypes) {
        this.methodName = methodName;
        this.paramCount = paramCount;

        if (paramTypes == null) {
            if (paramCount == 0) {
                this.paramTypes = new Class[] { String.class };
            } else {
                this.paramTypes = new Class[paramCount];
                Arrays.fill(this.paramTypes, String.class);
            }
        } else {
            this.paramTypes = paramTypes;
            boolean error = paramCount == 0 ? paramTypes.length - 1 != 0 : paramCount != paramTypes.length;
            /*if (paramCount == 0) {
                error = (paramTypes.length-1 != 0);
            }
            else {
                error = paramCount != paramTypes.length;
            }*/
            if (error) {
                throw new IllegalArgumentException("参数个数 paramCount 必须和 paramTypes 数组长度一致");
            }
        }
    }

    /**
     * 设置被调用的方法的 参数
     * @param uri
     * @param qName
     * @param attributes
     * @throws Exception
     */
    @Override
    public void begin(String uri, String qName, Attributes attributes) throws Exception {
        if (paramCount > 0) {
            // 参数容器
            Object[] parameters = new Object[paramCount];
            // 放入digester的对象栈
            digester.push(parameters); /** 在end()方法的时候*/
        }
    }

    /**
     * 获取节点内容， 这里就是 参数的value
     * @param namespace
     * @param name
     * @param text
     * @throws Exception
     */
    @Override
    public void body(String namespace, String name, String text) throws Exception {
        bodyText = text.trim();
    }

    /**
     * 运行方法了
     * @param uri
     * @param qName
     * @throws Exception
     */
    @Override
    public void end(String uri, String qName) throws Exception {
        Object[] parameters;

        if (paramCount > 0) { // 有参数
            // 参数出栈
            parameters = (Object[]) digester.pop();
        } else {
            // 标签里面的内容为空
            if (bodyText == null || bodyText.length() == 0) return;
            parameters = new Object[1];
            parameters[0] = bodyText;
        }
        // 根据配置参数类型，进行适当的转换
        for (int i = 0; i < paramTypes.length; i++) {
            Object param = parameters[i];
            parameters[i] = convert((String)param, paramTypes[i].getName());
        }


        //这里对象不需要删除掉，会在<servlet>标签结束的地方进行 出栈，删除掉这个类对象，
        // 不会影响到其他地方，而且可能会用到多次，标签里面有多个方法需要调用
        Object target = digester.peek();

        // 运行方法
        invokeMethod(target, methodName, parameters, paramTypes);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CallMethodRule[");
        sb.append("methodName=").append(methodName);
        sb.append(", paramCount=").append(paramCount);
        sb.append(", paramTypes={");
        if (paramTypes != null) {
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(paramTypes[i].getName());
            }
        }
        sb.append("}").append("]");
        return sb.toString();
    }
}
