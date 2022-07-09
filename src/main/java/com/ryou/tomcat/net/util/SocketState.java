package com.ryou.tomcat.net.util;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/7 13:32
 */
public enum SocketState {
    /** 长连接 */
    OPEN,
    /** 继续读取 */
    LONG,
    /** 发送 */
    WRITE,
    /** 断开连接 */
    CLOSED
}
