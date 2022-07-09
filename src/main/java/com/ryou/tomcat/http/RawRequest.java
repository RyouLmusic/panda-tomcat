package com.ryou.tomcat.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 16:02
 */
public class RawRequest implements Recyclable {
    /**
     * 请求方法
     */
    private String method; // GET POST ..
    private String uri; // /xxx.jsp    /j2ee2/hello

    private byte[] query; // 存储原始字节，对特殊的参数处理
    private int queryStartPos = -1;

    private String protocol; // HTTP/1.1

    private String contentType;
    private int contentLength = -1;
    /**
     * 是否装载 参数
     */
    private boolean loadedParameters = false;
    private boolean parseParamFail = false;
    private HashMap<String, String> parameters = new HashMap<>();
    private HashMap<String, String> headers = new HashMap<>();

    private HashMap<String, Object> attributes = new HashMap<>();

    /**
     * 通过 名字获取 请求头里面的内容
     * @param name 请求头的名字 如:  accept-language:zh-CN,zh;q=0.9 通过accept-language获取zh-CN,zh;q=0.9
     * @return zh-CN,zh;q=0.9
     */
    public String getHeader(String name) {
        return headers.get(name);
    }
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }
    public String removeHeader(String name) {
        return headers.remove(name);
    }

    private ActionHook hook;
    public void hook(ActionHook hook) {
        this.hook = hook;
    }

    /**
     * 获取数据的操作
     * @param action
     * @param param
     */
    public void action(ActionHook.ActionCode action, Object param) {
        if (hook != null) {
            if (param == null)
                hook.action(action, this);
            else
                hook.action(action, param);
        }
    }
    public boolean isParseParamFail() {
        return parseParamFail;
    }
    public void setParseParamFail(boolean parseParamFail) {
        this.parseParamFail = parseParamFail;
    }
    public HashMap<String, String> getParameters() {
        // 查询参数是否已经装配好，如果没装载，在此处进行 装载参数
        if (!loadedParameters) {
            action(ActionHook.ActionCode.PARSE_PARAMS, null);
        }
        return parameters;
    }

    public String getContentType() {
        if (contentType == null) {
            // 有可能含有 ; charset=utf-8
            contentType = headers.get("content-type");
        }
        return contentType;
    }

    @Override
    public void recycle() {
        contentType = null;
        contentLength = -1;

        loadedParameters = false;
        parseParamFail = false;
        parameters.clear();
        headers.clear();
        attributes.clear();
        queryStartPos = -1;
        if (query != null) {
            query = null;
        }
    }

    public Charset getEncoding() {
        String type = getContentType();
        if (type != null) {
            int start = type.indexOf("charset=");
            if (start >= 0) {
                String encoding = type.substring(start + 8);
                return Charset.forName(encoding.trim());
            }
        }
        return StandardCharsets.UTF_8; // 默认 utf-8 编码
    }

    public int getContentLength() {
        if (contentLength > 0) return contentLength;

        String v = getHeader("content-length");
        if (v != null) {
            return Integer.parseInt(v);
        }
        return -1;
    }

    // Getter&Setter
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * /j2ee2/hello
     * @return /j2ee2/hello
     */
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public byte[] getQuery() {
        return query;
    }
    public int getQueryStartPos() {
        return queryStartPos;
    }
    public void setQueryStartPos(int queryStartPos) {
        this.queryStartPos = queryStartPos;
    }
    public void setQuery(byte[] query) {
        this.query = query;
    }
    public String getProtocol() {
        return protocol;
    }
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * 返回参数是否已经装配好
     * @return true：说明已经装配好了参数
     */
    public boolean isLoadedParameters() {
        return loadedParameters;
    }

    /**
     * set 方法
     * @param loadedParameters 是否已经装载好参数
     */
    public void setLoadedParameters(boolean loadedParameters) {
        this.loadedParameters = loadedParameters;
    }
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }
    public HashMap<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Request headers: \r\n");
        builder.append(method).append(" ");
        builder.append(uri);
        if (query != null && query.length > 0) {

            builder.append("?").append(new String(query));
        }
        builder.append(" ").append(protocol).append("\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.append(header.getKey()).append(":").append(header.getValue()).append("\r\n");
        }
        builder.append("\r\n");
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            builder.append(param.getKey()).append("=").append(param.getValue()).append("\r\n");
        }

        return builder.toString();
    }
}
