package com.ryou.tomcat.http;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 16:21
 *
 * 可回收重复利用的对象
 */
public interface Recyclable {

    /**
     * 回收释放资源，供下一次请求使用
     */
    void recycle();
}
