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
package com.ryou.tomcat.container.servletx;


import com.ryou.tomcat.container.core.context.Context;
import com.ryou.tomcat.container.core.Wrapper;
import com.ryou.tomcat.container.session.Manager;
import com.ryou.tomcat.container.session.Session;
import com.ryou.tomcat.http.RawRequest;
import com.ryou.tomcat.http.Recyclable;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 这个对象最终会传到 Servlet 的 service 方法中，主要功能：<br>
 * <p>
 *  - 对 Session、Cookie 和 请求参数的处理进行了实现<br>
 *  - 支持 ServletInputStream 输入流<br>
 *
 */
public class Request implements HttpServletRequest, Recyclable {
	private Response response;
    public void setResponse(Response response) {
        this.response = response;
    }


    // URL 映射结果
	private Context context;
    public Context getContext() {
        return context;
    }
    public void setContext(Context context) {
        this.context = context;
    }


	private Wrapper wrapper;
    public Wrapper getWrapper() {
        return wrapper;
    }
    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }


	private RawRequest rawReq;
    public void setRawReq(RawRequest rawReq) {
        this.rawReq = rawReq;
    }

    /**
     * 管理session TODO
     */
    private Session session = null;
    private String sessionId = null;

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * SimpleDateFormat函数语法：
     *    G 年代标志符、  y 年、  M 月、  d 日、  h 时 在上午或下午 (1~12)、  H 时 在一天中 (0~23)
     *    m 分、  s 秒、  S 毫秒、  E 星期、  D 一年中的第几天、 F 一月中第几个星期几、  w 一年中第几个星期、  W 一月中第几个星期、
     *    a 上午 / 下午 标记符、   k 时 在一天中 (1~24)、  K 时 在上午或下午 (0~11)、  z 时区
     */
	private final SimpleDateFormat[] formats = {
	        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    };


    /**
     * cookie的属性：
     *  name　字段为一个cookie的名称。
     *  value　字段为一个cookie的值。
     *  domain　字段为可以访问此cookie的域名。
     *  path　字段为可以访问此cookie的页面路径。
     *  expires/Max-Age  字段为此cookie超时时间。
     *  Size　字段 此cookie大小
     *  httponly属性  若此属性为true，则只有在http请求头中会带有此cookie的信息，而不能通过document.cookie来访问此cookie。
     *  secure　字段 设置是否只能通过https来传递此条cookie
     *
     *
     *
     * 请求时 Cookie 格式为：
     * Cookie: name1=value1; name2=value2; name3=value3 //多个 Cookie 之间用 `; ` 隔开
     *
     * 响应时 Cookie 格式为：
     * Set-Cookie: key1=value1; path=path; domain=domain; max-age=max-age-in-seconds; expires=date-in-GMTString-format; secure; httponly
     * Set-Cookie: key2=value2; path=path; domain=domain; max-age=max-age-in-seconds; expires=date-in-GMTString-format; secure; httponly
     *
     * key=value 名称、值的键值对
     * path=path 设置在哪个路径下生效，大部分时候设置为 /，这样可以在所有路径下生效
     * domain=domain 设置在哪个域名下生效，会验证 domain 的合法性
     * max-age=max-age-in-seconds 存活时间，一般跟 expires 配套使用
     * expires=date-in-GMTString-format 失效日期
     * secure 只在 HTTPS 下生效
     * httponly 只在 HTTP 请求中携带，JS 无法获取
     */
	protected Cookie[] cookies = null;



    @Override
    public void recycle() {
        context = null;
        wrapper = null;

        cookies = null;
        if (session != null) {
            session.endAccess();
        }
        session = null;
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        if (cookies == null) {
            // 从请求中获取cookie
            parseCookies();
        }
        return cookies;
    }

    private void parseCookies() {
        String cookiesStr = rawReq.getHeader("cookie");
        if (cookiesStr != null) {
            // 切割获取 多个cookie的集合  Cookie: name1=value1; name2=value2; name3=value3
            String[] cookiesArray = cookiesStr.split(";");
            int cookieCount = cookiesArray.length;
            if (cookieCount == 0) {
                return;
            }
            cookies = new Cookie[cookieCount];
            for (int i = 0; i < cookieCount; i++) {
                // name2=value2 去除空格并且 通过 '=' 切割 得到 各自cookie的name 和value值
                String[] temp = cookiesArray[i].trim().split("=");
                // 实例化一个cookie
                Cookie cookie = new Cookie(/*name*/temp[0], /*value*/temp[1]);
                cookies[i] = cookie;
            }
        }
    }

    /**
     * 该方法用于获取指定头字段的值，并将其按 GMT 时间格式转换为一个代表日期/时间的长整数，
     * 该长整数是自 1970 年 1 月 1 日 0 时 0 分 0 秒算起的以毫秒为单位的时间值
     * @param s
     * @return
     */
    @Override
    public long getDateHeader(String s) {
        String value = getHeader(s);
        if (value != null) {
            Date date = null;
            for (int i = 0; (date == null) && (i < formats.length); i++) {
                try {
                    date = formats[i].parse(value);
                } catch (ParseException ignored) { }
            }
            if (date != null) {
                // 转换成long的时间
                return date.getTime();
            }
        }
        return -1;
    }

    @Override
    public String getParameter(String name) {
        // 请求参数的解析放到了 RawRequest 中
        return rawReq.getParameters().get(name);
    }


    @Override
    public HttpSession getSession(boolean create) {

        if (session != null) {
            if (!session.isValid()) {
                session = null;
            } else {
                return session.getSession();
            }
        }

        Manager manager = getContext().getManager();
        if (sessionId != null) {
            session = manager.findSession(sessionId);
            if (session != null) {
                if (session.isValid()) {
                    session.access();
                    return session.getSession();
                } else {
                    session = null;
                }
            }
        }

        if (create) {
            session = manager.createSession();
            if (session == null) return null;

            // Add cookie
            Cookie cookie = new Cookie("JSESSIONID", session.getId());
            response.addCookie(cookie);

            session.access();
            return session.getSession();
        } else {
            return null;
        }
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public String getMethod() {
        return rawReq.getMethod();
    }

    @Override
    public String getRequestURI() {
        return rawReq.getUri();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }



    @Override
    public String getServletPath() {
        return wrapper.getWrapperPath();
    }

    @Override
    public void setAttribute(String name, Object o) {
        rawReq.setAttribute(name, o);
    }

    @Override
    public Object getAttribute(String name) {
        return rawReq.getAttributes().get(name);
    }

    /* 以下方法未实现 TODO */
    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
    }

    @Override
    public int getContentLength() {
        return rawReq.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getContentType() {
        return rawReq.getContentType();
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(rawReq.getParameters().keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        return 0;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }



    @Override
    public void removeAttribute(String name) {
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    @Override
    public String getHeader(String name) {
        return rawReq.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return null;
    }

    @Override
    public int getIntHeader(String name) {
        return 0;
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return "/" + context.getDocBase();
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return sessionId;
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String s, String s1) throws ServletException {

    }

    @Override
    public void logout() throws ServletException {

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public String toString() {
        return rawReq.toString();
    }

}
