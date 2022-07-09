package com.ryou.tomcat.http;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 16:24
 *
 * HTTP 协议相关标识符
 */
public final class HttpToken {

    // http 标识符
    public static final byte CR         = (byte) '\r';
    public static final byte LF         = (byte) '\n';
    public static final byte SP         = (byte) ' ';
    public static final byte COLON      = (byte) ':';
    public static final byte SEMI_COLON = (byte) ';';
    public static final byte QUESTION   = (byte) '?';


    public static final byte[] HTTP_1_1 = "HTTP/1.1 ".getBytes();
    public static final byte[] CRLF = "\r\n".getBytes();
    public static final byte[] END_CHUNK = "0\r\n\r\n".getBytes();

    public static final int SC_CONTINUE = 100;
    public static final int SC_OK = 200;
    public static final int SC_BAD_REQUEST = 400;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_INTERNAL_SERVER_ERROR = 500;
    public static final int SC_NOT_MODIFIED = 304;

    static String msg (int code) {
        switch (code) {
            case SC_OK: return " OK";
            case SC_CONTINUE: return " Continue";
            case SC_BAD_REQUEST: return " Bad Request";
            case SC_NOT_FOUND: return " Not Found";
            case SC_INTERNAL_SERVER_ERROR: return " Internal Server Error";
            case SC_NOT_MODIFIED: return " Not Modified";
            default: return "";
        }
    }
}
