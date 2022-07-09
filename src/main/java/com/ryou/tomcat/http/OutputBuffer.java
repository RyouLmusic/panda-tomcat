package com.ryou.tomcat.http;

import ch.qos.logback.classic.Logger;
import com.ryou.tomcat.net.Acceptor;
import com.ryou.tomcat.net.NioChannel;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 16:21
 *
 * 编码响应:
 * 可以交由getOutputStream使用
 */
public class OutputBuffer implements Recyclable {

    private final static Logger log = (Logger) LoggerFactory.getLogger(Acceptor.class);

    public static final byte[] HTTP_1_1 = "HTTP/1.1 ".getBytes();
    public static final byte[] CRLF_BYTES = "\r\n".getBytes();

    private ByteBuffer byteBuffer;
    private BodyCodec codec;

    private NioChannel socket;
    private RawResponse resp;

    public OutputBuffer(RawResponse resp) {
        this.resp = resp;
    }
    public void setSocket(NioChannel socket) {
        this.socket = socket;
        byteBuffer = socket.writeBuf();
        byteBuffer.clear();
    }
    /**
     * 将响应头写入到缓冲区
     *
     * @throws IOException
     */
    public void commit() throws IOException {
        resp.setCommitted(true);
        int pos = byteBuffer.position();

        // 1. 将状态行写入缓冲区
        byteBuffer.put(HttpToken.HTTP_1_1);
        int status = resp.getStatus();
        byteBuffer.put(String.valueOf(status).getBytes());

        byte[] msg = null;
        if (resp.getMessage() != null) {
            msg = resp.getMessage().getBytes(resp.getCharacterEncoding());
        } else {
            msg = HttpToken.msg(status).getBytes();
        }

        byteBuffer.put(msg);
        byteBuffer.put(HttpToken.CRLF);

        // 2. 将响应头域写入缓冲区
        for (Map.Entry<String, String> header : resp.headers().entrySet()) {
            byte[] name = header.getKey().getBytes();
            if ("Set-Cookie".equalsIgnoreCase(header.getKey())) {
                for (String cookie : header.getValue().split(";")) {
                    writeHeader(name, cookie.getBytes());
                }
            } else {
                writeHeader(name, header.getValue().getBytes());
            }
        }
        byteBuffer.put(HttpToken.CRLF);

        log.debug("将响应头部 [{}B] 数据写入提交到底层缓冲区", (byteBuffer.position() - pos + 1));
    }

    private void writeHeader(byte[] name, byte[] value) {
        byteBuffer.put(name);
        byteBuffer.put(HttpToken.COLON);
        byteBuffer.put(HttpToken.SP);
        byteBuffer.put(value);
        byteBuffer.put(HttpToken.CRLF);
    }

    /**
     * 写入响应体数据前，响应头确认已写入缓冲区
     *
     * @param src 待写入数据
     * @throws IOException
     */
    public void writeBody(ByteBuffer src) throws IOException {
        if (!resp.isCommitted()) {
            resp.action(ActionHook.ActionCode.COMMIT, null);
        }

        if (src.remaining() > 0) {
            log.debug("写入响应体数据 [{}B]", src.remaining());
            codec.doWrite(this, src);
        }
    }

    public void end() throws IOException {
        if (!resp.isCommitted()) {
            resp.action(ActionHook.ActionCode.COMMIT, null);
        }

        if (codec != null) {
            codec.endWrite(this);
        }
        flush();
    }

    public void write(byte[] b) throws IOException {
        write(ByteBuffer.wrap(b));
    }
    public void write(ByteBuffer b) throws IOException {
        write(b, false);
    }
    // 写入通道待发送缓冲区
    public void write(ByteBuffer src, boolean flip) throws IOException {
        if (flip) src.flip();
        while (src.hasRemaining()) {
            // 无空间可供写入
            if (byteBuffer.remaining() == 0) {
                socket.flush(); // 把数据发送到客户端
            }
            byteBuffer.put(src);
        }
        src.clear();
        // 以防超时
        socket.access();
    }
    public void flush() throws IOException {
        socket.flush();
    }
    public void setBodyCodec(BodyCodec body) {
        this.codec = body;
    }

    public void setRawResponse(RawResponse response) {
        resp = response;
    }
    @Override
    public void recycle() {
        resp.recycle();

        byteBuffer.clear();
        codec = null;
    }
}
