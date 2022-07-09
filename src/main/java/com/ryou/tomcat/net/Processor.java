package com.ryou.tomcat.net;

import com.ryou.tomcat.net.util.SocketState;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/7 12:10
 *
 * 适配不同协议的处理器
 */
public interface Processor {
    SocketState process(NioChannel socket);
}
