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

import com.ryou.tomcat.utils.digester.rule.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.*;

/**
 * 依据配置好的规则使用 Sax 解析 XML，只对节点名和属性处理，不处理内容
 *
 * SAX解析步骤：
 *
 * 1.创建一个SAXParserFactory对象SAXParserFactory factory=SAXParserFactory.newInstance();
 *
 * 2.获得解析器SAXParser parser=factory.newSAXParser();
 *
 * 3.调用解析方法解析xml，这里的第一个参数可以传递文件、流、字符串；第二个参数我们先定义一个用于实现解析类，如SAXParserHandler。
 *
 * 4.创建一个SAXParserHandler类来继承DefaultHandler并重写方法。即下面提到的五个必要方法，执行顺序按以下序号排列：
 *
 * 1.startDocument()：开始处理文档；
 *
 * 2.startElement(String uri, String localName, String qName, Attributes attributes)：处理元素的开始；
 *
 * 3.characters(char[] ch, int start, int length)：用来读取节点内容；
 *
 * 4.endElement(String uri, String localName, String qName)：元素处理结束；
 *
 * 5.endDocument()：文档处理结束。
 *
 *
 *   DefaultHandler 解析XML 的非根node是按顺序的四步
 *  第一步：startElement.   (eg:startElement localName :  qName : age)
 *  第二步 :  characters       (eg:characters in age = 25)
 *  第三步 :  endElement     (eg: endElement in )
 *  第四步 :  characters       (eg: characters in null =  !)
 *
 */
public class Digester extends DefaultHandler {

    final static Logger log = LoggerFactory.getLogger(Digester.class);

    private Object root;

    /** 默认使用 Thread.currentThread().getContextClassLoader() */
    protected ClassLoader classLoader;

    /** 对象栈 */
    private final LinkedList<Object> stack = new LinkedList<>();

    /** 解析过程中节点对应匹配的规则  也是一个栈 */
    private final LinkedList<List<Rule>> matches = new LinkedList<>();


    /** 当前节点元素包含的文本  同是一个栈 */
    private final LinkedList<StringBuilder> bodyTexts = new LinkedList<>();

    /** 配置的规则 */
    /**
     * ContextConfig的init()方法里面调用的call方法的 pattern 为key，相同的key会有多个rule
     * 如：
     *         webXmlParser.addObjectCreate("web-app/filter", "com.ryou.tomcat.container.servletx.FilterWrapper");
     *         webXmlParser.addSetNext("web-app/filter","addFilterWrapper");
     *
     * 通过这些key(或者pattern)进行匹配
     */
    private final HashMap<String, List<Rule>> rules = new HashMap<>();

    /** 当前匹配的 pattern */
    private String match = "";


    /**
     * 读取web.xml的方法
     * @param input
     * @return
     * @throws IOException
     * @throws SAXException
     */
    public Object parse(InputSource input) throws IOException, SAXException {
        getXMLReader().parse(input);
        return root;
    }

    /**
     * sax 解析工厂
     * @return
     */
    private XMLReader getXMLReader() {
        XMLReader reader = null;
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            reader = parser.getXMLReader();
        } catch (Exception e) {
            log.error("", e);
        }
        // 设置操作（this）
        reader.setContentHandler(this);
        return reader;
    }

    // Sax method
    /**
     * 解析开始
     * @throws SAXException
     */
    @Override
    public void startDocument() throws SAXException {
    }

    /**
     * 每个节点解析开始 运行的方法
     * @param uri
     * @param localName
     * @param qName
     * @param attributes
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        log.debug("startElement({},{},{})", uri, localName, qName);

        // 入栈一个保存元素节点内容的 StringBuilder,
        // 不在characters()里面进行操作的原因：characters()在解析一个节点的时候会调用多次，为了不进行多次push
        bodyTexts.push(new StringBuilder());

        // 连接节点   得到结果：web-app/context-param/param-name
        StringBuilder sb = new StringBuilder(match);
        // 在添加下一个节点的名称之前 ，先加 '/' 进行分割
        if (match.length() > 0) {
            sb.append('/');
        }
        // 把 现在正在解析的节点名称 添加进行sb里面
        sb.append(qName);
        // xml文件  节点/子节点    的模式进行连接，  保存起来，给下一个节点解析的时候 可以继续添加
        match = sb.toString();

        log.info("  New match='[ {} ]'", match);
        // 进行匹配 这些节点 的规则
        List<Rule> matchRules = matchRules(match);
        // 入栈，只有在解析此节点的时候有用到，结束之后就退栈，  先入的节点  后处理
        matches.push(matchRules); // matchRules会在endElement里面出栈进行使用

        // 迭代 此节点里面的 规则 ，调用其规则的begin方法进行处理
        if (matchRules != null && matchRules.size() > 0) {
            for (Rule matchRule : matchRules) {
                try {
                    log.info("  执行 [{}]  的 begin() 方法 ", matchRule);
                    // 执行每个 "规则" 里面的begin方法
                    matchRule.begin(uri, qName, attributes);
                } catch (Exception e) {
                    log.error("Begin event threw exception", e);
                    throw new SAXException(e);
                }
            }
        } else {
            log.debug("  No rules found matching '{}'.", match);
        }
    }

    /**
     * 获取节点的内容
     * 在解析非根节点的时候，会在startElement()方法和endElement()后面进行调用，第一次调用 有内容，第二次调用是空
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        log.debug("characters(...)");

        // 获取当前元素节点关联的 StringBuilder，添加内容
        StringBuilder bodyText = bodyTexts.peek();

        // 将此节点标签的内容添加到 bodyText里面
        // 内容如：  HelloServlet
        Objects.requireNonNull(bodyText).append(ch, start, length);

    }

    /**
     * 解析一个节点结束的时候调用
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // 节点标签 规则 出栈，可以来使用 规则 的body方法和end方法
        List<Rule> matchRules = matches.pop();
        // 节点内容文本 出栈
        StringBuilder bodyText = bodyTexts.pop();

        // 迭代调用每个规则的 body方法
        if (matchRules != null && matchRules.size() > 0) {
            for (Rule matchRule : matchRules) {
                try {
                    log.debug("执行 [{}]  的 body() 方法", matchRule);
                    matchRule.body(uri, qName, bodyText.toString());
                } catch (Exception e) {
                    log.error("Body event threw exception", e);
                    throw new SAXException(e);
                }
            }
        } else {
            log.debug("  No rules found matching '{}'.", match);
        }

        //
        for (int i = 0; i < matchRules.size(); i++) { // 倒叙遍历
            int j = (matchRules.size() - 1) - i;
            try {
                Rule rule = matchRules.get(j);
                log.debug("执行 [{}]  的 end() 方法 " , rule);
                rule.end(uri, qName);
            } catch (Exception e) {
                log.error("End event threw exception", e);
                throw new SAXException(e);
            }
        }
        // 恢复上一个匹配的表达式
        int slash = match.lastIndexOf('/');
        if (slash >= 0) {
            // web-app/context-param/param-name   --->   web-app/context-param
            match = match.substring(0, slash);
        } else {
            // '' --> ''
            match = "";
        }
    }

    /**
     * 标识解析结束
     * @throws SAXException
     */
    @Override
    public void endDocument() throws SAXException {
        match = "";
        stack.clear();
    }
    // End Sax method

    public String match() {
        return match;
    }

    /**
     * 对象栈的操作： 入栈
     * @param obj 实例化的对象
     */
    public void push(Object obj) {
        if (stack.size() == 0) {
            root = obj;
        }
        stack.push(obj);
    }

    public Object pop() {
        return stack.pop();
    }

    public Object peek() {
        return stack.peek();
    }

    public Object peek(int index) {
        return stack.get(index);
    }


    // --------------------------------------------  添加rule的方法


    public void addSetFields(String pattern) {
        SetFieldsRule setFieldsRule = new SetFieldsRule();
        setFieldsRule.setDigester(this);
        addRule(pattern, setFieldsRule);
    }

    public void addObjectCreate(String pattern, String clazz) {
        ObjectCreateRule objectCreateRule = new ObjectCreateRule(clazz);
        objectCreateRule.setDigester(this);
        addRule(pattern, objectCreateRule);
    }

    public void addSetNext(String pattern, String methodName) {
        SetNextRule setNextRule = new SetNextRule(methodName);
        setNextRule.setDigester(this);
        addRule(pattern, setNextRule);
    }

    public void addSetNext(String pattern, String methodName, String paramType) {
        SetNextRule setNextRule = new SetNextRule(methodName, paramType);
        setNextRule.setDigester(this);
        addRule(pattern, setNextRule);
    }

    public void addCallMethod(String pattern, String methodName, int paramCount) {
        addCallMethod(pattern, methodName, paramCount, null);
    }

    public void addCallMethod(String pattern, String methodName, int paramCount, Class<?>[] paramsType) {
        CallMethodRule callMethod = new CallMethodRule(methodName, paramCount, paramsType);
        callMethod.setDigester(this);
        addRule(pattern, callMethod);
    }

    public void addCallParam(String pattern, int paramIndex) {
        addCallParam(pattern, paramIndex, null);
    }

    public void addCallParam(String pattern, int paramIndex, String attributeName) {
        CallParamRule callParam = new CallParamRule(paramIndex, attributeName);
        callParam.setDigester(this);
        addRule(pattern, callParam);
    }

    public void addCallMethodMultiRule(String pattern, String methodName, int paramCount, int multiParamIndex) {
        CallMethodMultiRule callMethodMulti = new CallMethodMultiRule(methodName, paramCount, multiParamIndex);
        callMethodMulti.setDigester(this);
        addRule(pattern, callMethodMulti);
    }

    public void addCallParamMultiRule(String pattern, int paramIndex) {
        CallParamMultiRule callParamMulti = new CallParamMultiRule(paramIndex);
        callParamMulti.setDigester(this);
        // 添加到解析规则里面
        addRule(pattern, callParamMulti);
    }

    /**
     * 将xml的解析规则加入
     * @param pattern
     * @param rule
     */
    // rules
    public void addRule(String pattern, Rule rule) {

        int patternLength = pattern.length();
        // 去除结尾为 '/'的pattern
        if (patternLength > 1 && pattern.endsWith("/")) {
            pattern = pattern.substring(0, patternLength - 1);
        }

        List<Rule> list = rules.get(pattern);
        if (list == null) {
            list = new ArrayList<>();
            rules.put(pattern, list);
        }
        list.add(rule);
    }

    /**
     * 匹配 pattern,从 rules中获取rule的list
     * @param pattern 例如：web-app/filter
     * @return Rule列表
     */
    public List<Rule> matchRules(String pattern) {
        // 获取 这种pattern的  规则
        List<Rule> rulesList = rules.get(pattern);
        // 如果获取的规则为空 直接new一个 List<Rule>
        if (rulesList == null) {
            rulesList = new ArrayList<>();
        }
        return rulesList;
    }

    /**
     * 设置此加载的项目的  类加载器ClassLoader
     * @param cl
     */
    public void setClassLoader(ClassLoader cl) {
        classLoader = cl;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

}
