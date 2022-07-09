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
package com.ryou.tomcat.utils.digester;

import org.xml.sax.Attributes;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 解析过程中遇到 xml 元素时的处理规则
 */
public abstract class Rule {
    
    protected Digester digester = null;

    public Rule() {
    }
    
    public void begin(String uri, String qName, Attributes /*接口的XML属性列表*/ attributes) throws Exception {
    }
    public void body(String namespace, String name, String text) throws Exception {
    }
    public void end(String uri, String qName) throws Exception {
    }

    public void setDigester(Digester digester) {
        this.digester = digester;
    }

    /**
     * 加载 webapp下面的类，只是进行加载，没有进行实例化 newInstance
     * @param className 类的位置
     * @return 类的 Class类
     * @throws ClassNotFoundException 没有找到这个类
     */
    protected Class<?> loadClass(String className) throws ClassNotFoundException {
        ClassLoader loader = digester.getClassLoader();
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        Class<?> clazz = loader.loadClass(className);
        return clazz;
    }
    
    // Reflection - 反射调用
    /**
     * 实例化className的类-->  对象
     * @param className
     * @return
     */
    protected Object newInstance(String className) {
        try {
            Class<?> clazz = loadClass(className);
            return clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 调用方法
     * @param target Object 调用的方法所属的类
     * @param methodName String 调用的方法名称
     * @param args Object[] 参数
     * @param argsType Object[] 参数类型
     */
    protected void invokeMethod(Object target, String methodName, Object[] args, Object[] argsType) {
        Method md = null;
        // 获取类的方法集合
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            // 通过方法的名称和方法的参数 获取方法
            if (methodName.equals(method.getName()) && (Arrays.equals(argsType, method.getParameterTypes()))) {
                md = method;
                break;
            }
        }

        if (md != null) {
            try {
                // 调用方法
                md.invoke(target, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 调用关于 set方法
     * @param target Object set方法所属的类
     * @param methodName String 方法的名称
     * @param value String 进行set的参数
     */
    protected void invokeSetMethod(Object target, String methodName, String value) {
        // 要被调用的方法
        Method targetMethod = null;
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            // 通过名称匹配
            if (methodName.equals(method.getName())) {
                targetMethod = method;
            }
        }
        try {
            // 转换参数
            Object param = convert(value, targetMethod.getParameterTypes()[0].getName());
            // 调用方法
            targetMethod.invoke(target, param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将 String 类型的参数转换成 真正的类型
     * @param param 参数值
     * @param paramTypeName 参数类型
     * @return 正确的参数值
     */
    protected Object convert(String param, String paramTypeName) {
        Object result = null;
        if ("java.lang.String".equals(paramTypeName)) {
            result = param;
        } else if ("java.lang.Integer".equals(paramTypeName) || "int".equals(paramTypeName)) {
            result = Integer.valueOf(param);
        } else if ("java.lang.Boolean".equals(paramTypeName) || "boolean".equals(paramTypeName)) {
            result = Boolean.valueOf(param);
        }
        return result;
    }
    
}
