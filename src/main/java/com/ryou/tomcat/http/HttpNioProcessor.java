package com.ryou.tomcat.http;

import ch.qos.logback.classic.Logger;
import com.ryou.tomcat.http.codec.ChunkedCodec;
import com.ryou.tomcat.http.codec.IdentityCodec;
import com.ryou.tomcat.net.Acceptor;
import com.ryou.tomcat.net.NioChannel;
import com.ryou.tomcat.net.Processor;
import com.ryou.tomcat.net.util.SocketState;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/25 16:12
 *
 * 最终 请求响应的处理器
 */
public class HttpNioProcessor implements Processor, ActionHook {

    private final static Logger log = (Logger) LoggerFactory.getLogger(Acceptor.class);

    private final InputBuffer inBuffer;
    private final OutputBuffer outBuffer;

    private final RawRequest request;
    private final RawResponse response;

    /**
     * 适配器，用于连接 Endpoint 和 Container
     */
    private Adapter adapter;

    /**
     * 在实例化HttpNioProcessor的时候 进行调用，初始化 Endpoint和Container 的 适配器
     * @param adapter 适配器
     */
    public void setAdaptor(Adapter adapter) {
        this.adapter = adapter;
    }

    private boolean keepAlive = true;
    private boolean error = false;

    /** 一个长连接最多处理多少个 Request，-1 表示不限制 */
    private int maxKeepAliveRequests = -1;

    public HttpNioProcessor() {
        request = new RawRequest();
        inBuffer = new InputBuffer(request);
        request.hook(this);

        response = new RawResponse();
        outBuffer = new OutputBuffer(response);
        response.hook(this);
        maxKeepAliveRequests = 10;
    }

    @Override
    public void action(ActionCode actionCode, Object... param) {

        switch (actionCode) {
            case COMMIT:
                if (!response.isCommitted()) {
                    try {
                        prepareResponse();
                        outBuffer.commit();
                    } catch (IOException e) {
                        error = true;
                    }
                }
                break;
            case CLOSE:
                action(ActionCode.COMMIT);
                try {
                    outBuffer.end();
                } catch (IOException e) {
                    error = true;
                    e.printStackTrace();
                }
                break;
            case PARSE_PARAMS:
                try {
                    // 如果设置参数失败： 进行再次进行
                    inBuffer.parseRequestLineAndHeaders();
                    inBuffer.readAndParseParameters();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case WRITE_BODY:
                action(ActionCode.COMMIT);
                try {
                    outBuffer.writeBody((ByteBuffer)param[0]);
                } catch (IOException e) {
                    error = true;
                    e.printStackTrace();
                }
                break;
            case FLUSH:
                action(ActionCode.COMMIT);
                try {
                    outBuffer.flush();
                } catch (IOException e) {
                    error = true;
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public SocketState process(NioChannel socket) {
        // 解析通道里的请求
        inBuffer.setSocket(socket);
        // 生成通道里的响应
        outBuffer.setSocket(socket);

        int keepAliveLeft = maxKeepAliveRequests;

        while (!error && keepAlive) {
            // 1. 解析请求头
            try {
                // 如果请求头解析失败，返回LONG，让Handler再次注册事件
                if (!inBuffer.parseRequestLineAndHeaders()) { // 调用了此方法之后，inBuffer里面的BufferedReader已经 读取到请求体部分了
                    return SocketState.LONG;
                }
            } catch (IOException e) { // 发生异常 返回停止的信号
                // 这里异常通常是 连接关闭和 socket 超时，EOFException SocketTimeoutException
                return SocketState.CLOSED;
            }

            // 2. 校验请求头数据，设置请求体解码器
            // 校验
            checkRequest();
            // 设置解码器
            prepareRequest();

            // 解析post方法请求体里面的参数 TODO 很多解码方式还未实现
            inBuffer.readAndParseParameters();
            log.info("请求数据读取并解析完毕\r\n======Request======\r\n{}\r\n===================", request);

            // 3. 检查是否还要保持连接
            if (maxKeepAliveRequests > 0 && --keepAliveLeft == 0) {
                keepAlive = false;
            }
            // 4. 交给容器处理请求并生成响应
            if (!error) {
                try {
                    log.debug("交给容器处理请求并生成响应");
                    adapter.service(request, response);
                    System.out.println(response);
                } catch (Exception e) {
                    error = true;
                    e.printStackTrace();
                }
            }
            // TODO 检查客户端的数据是否发送完毕

            try {
                outBuffer.end();
            } catch (Throwable t) {
                log.error("Error finishing response", t);
                error = true;
                response.setStatus(500);
            }

            // 5. 回收释放资源处理下一个请求
            inBuffer.recycle();
            outBuffer.recycle();

            // 6. 返回保持连接的状态
            if (!error && keepAlive) {
                return SocketState.OPEN;
            }
        }// end while
        return SocketState.CLOSED;
    }



    /**
     * 检查请求是否合法，
     */
    private void checkRequest() {
        // 0. 检查协议版本
        String version = request.getProtocol();
        if (!"HTTP/1.1".equalsIgnoreCase(version)) {
            error = true;
//            Send 505; Unsupported HTTP version TODO
//            response.setStatus(505);
        }

        // 1. 检查是否要保持连接
        String conn = request.getHeader("connection");
        if (conn == null || "close".equals(conn)) {
            keepAlive = false;
        } else if ("keep-alive".equals(conn)) {
            keepAlive = true;
        }
        // 2. 检查 expect 头
        // 3. 检查 host
        String host = request.getHeader("host");
        if (host == null || host.length() <= 0) {
            error = true;
//            400 - Bad request TODO
//            response.setStatus(400);
        }
    }

    /**
     * 设置请求体解码器
     */
    private void prepareRequest() {

        // 3. 检查传输编码
        boolean contentDelimitation = false;
        // 检测是否有传输编码格式
        String transferEncoding = request.getHeader("transfer-encoding");
        // 如果编码格式为chunked
        if ("chunked".equalsIgnoreCase(transferEncoding)) {
            contentDelimitation = true;
            inBuffer.setBodyCodec(new ChunkedCodec(request.getContentType()));
        }

        // 4. 检查是否有content-length头
        int contentLength = request.getContentLength();
        if (contentLength >= 0) {
            if (contentDelimitation) {
                // 有了 chunked 编码，contentLength 无效
                request.removeHeader("content-length");
                request.setContentLength(-1);
            } else {
                inBuffer.setBodyCodec(new IdentityCodec(contentLength, request.getContentType()));
            }
        }

    }

    private void prepareResponse() throws IOException {
        // 0. 检查是否有响应体
        int statusCode = response.getStatus();
        if ((statusCode == 204) || (statusCode == 205)
                || (statusCode == 304)) {
            // No entity body
            response.setContentLength(-1);
        } else {
            // 1. Content-Type
            String contentType = response.getContentType();
            if (contentType != null) {
                response.addHeader("Content-Type", contentType);
            }
            String contentLanguage = response.getContentLanguage();
            if (contentLanguage != null) {
                response.addHeader("Content-Language", contentLanguage);
            }
            // 2. 设置响应体编码处理器
            int contentLength = response.getContentLength();
            if (contentLength != -1) {
                response.addHeader("Content-Length", String.valueOf(contentLength));
                // TODO 添加了contentType
                outBuffer.setBodyCodec(new IdentityCodec(contentLength, contentType));
            } else {
                response.addHeader("Transfer-Encoding", "chunked");
                // TODO 添加了contentType
                outBuffer.setBodyCodec(new ChunkedCodec(contentType));
            }
        }

        response.addHeader("Server", "cytomcat/1.0");
    }

}


