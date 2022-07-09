package com.ryou.tomcat.http;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 16:00
 *
 * 容器对 Processor 请求操作的回调机制
 */
public interface ActionHook {

    /**
     * 容器对 Processor 的回调动作
     */
    enum ActionCode {
        ACK,

        /** 请求提交响应头数据到缓冲区 */
        COMMIT,

        /** 请求读取并解析请求参数 */
        PARSE_PARAMS,

        /** 请求写入响应体数据 */
        WRITE_BODY,

        /** 请求读取请求体数据 */
        READ_BODY,

        /** 请求将响应发送到客户端 */
        FLUSH,

        /** 响应处理完毕 */
        CLOSE
    }

    /**
     * 请求 Processor 处理一个动作
     *
     * @param actionCode 动作类型
     * @param param 动作发生时关联的参数
     */
    public void action(ActionCode actionCode, Object... param);
}
