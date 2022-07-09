package com.ryou.tomcat.http;

import ch.qos.logback.classic.Logger;
import com.ryou.tomcat.net.NioChannel;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 16:24
 *
 * 使用有限状态机解析 HTTP 协议请求行和请求体
 * 请求行--请求头----请求体
 */
public class InputBuffer implements Recyclable, BufferHolder {


    /**
     * 请求解析状态
     */
    public enum ParseStatus {
        START, // 解析开始
        METHOD, // 解析请求方法
        URI, // 解析请求 URI
        VERSION, // 解析协议版本
        QUERY, // 解析查询参数
        HEADER_NAME, // 解析头域名称
        HEADER_VALUE, // 解析头域值
        HEADER_END, // 解析一个头域完毕
        DONE // 解析完成
    }


    final Logger log = (Logger) LoggerFactory.getLogger(InputBuffer.class);

    /** 当前解析状态 */
    private ParseStatus status = ParseStatus.METHOD;

    /** 正在解析请求行 */
    protected boolean parsingRequestLine = false;
    /** 正在解析请求头域 */
    private boolean parsingHeader = false;
    /**
     * 请求头、请求行的最大 大小
     */
    private int maxHeaderSize = 8192;

    /** 请全体解析器 */
    private BodyCodec codec;
    /** 原型请求 */
    private final RawRequest request;
    /** 请求通信通道 */
    private NioChannel socket;

    /** byteBuffer 引用的是 NioChannel 内部的  readBuf */
    private ByteBuffer readBuf;

    /** Buffer转成 bufferedReader*/
    private BufferedReader bufferedReader;

    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }
    /**
     * 请求头 信息在字节数组{@link ByteBuffer} 中结束的位置，即请求体数据开始的位置
     * Request Header End
     */
    private int reqHeaderEndIndex;

    public InputBuffer(RawRequest request) {
        this.request = request;
    }


    /**
     * 将NioChannel的属性 设置
     * @param socket {@link NioChannel}
     */
    public void setSocket(NioChannel socket) {
        this.socket = socket;
        readBuf = socket.readBuf(); // 从通道里面带来的 buffer给readBuf
        readBuf.clear(); // 清理掉buffer里面的所有东西
    }


    private StringBuilder sb = new StringBuilder();

    /**
     * 将sb里面的内容取出，并且将其置为空
     * @return String
     */
    private String takeString() {
        String str = sb.toString();
        sb.setLength(0);
        return str;
    }


    /**
     * 使用状态机的方法，遍历字节解析请求头（解析时没有进行严谨性校验）
     *
     * @return true - 读取完成，false - 读取到部分请求头
     * @throws IOException
     */
    public boolean parseRequestLineAndHeaders() throws IOException {
        log.info("解析请求行和请求 Headers");

        // 检查是否读取到信息
        if (!fill()) return false;

        // 进行解析请求行和请求头
        bufferedReader = doByteByteBufferToBufferReader(readBuf);
        // 获取请求行
        String requestLine = bufferedReader.readLine();
        String[] lines = requestLine.split(" "); // TODO HttpToken
        // 请求方法 GET / POST
        String method = lines[0];
        request.setMethod(method);
        // 处理请求路径和请求参数的分离
        String urlAndParameters = lines[1];
        try {
            URI uri = new URI(urlAndParameters);
            // 路径
            String path = uri.getPath();
            request.setUri(path);
            // 参数
            String query = uri.getQuery();
            // 如果请求行里面有参数，将参数放入request里面
            if (query != null && !query.equals("")) request.setQuery(query.getBytes(StandardCharsets.UTF_8));
        } catch (URISyntaxException e) {
            log.error("不是一个资源路径");
        }

        // HTTP1.1版本
        String protocol = lines[2];
        request.setProtocol(protocol);

        /* 请求头解析 */

        String content;
        while (!((content = bufferedReader.readLine()).trim()).equals("")) {
            String[] header = content.split(": ");
            // 请求头 属性段 的名字
            String headerName = header[0].toLowerCase();
            String headerValue = header[1];
            // 添加到request里面
            request.addHeader(headerName, headerValue);
        }

//        log.info("请求头部数据读取并解析完毕\r\n======Request======\r\n{}\r\n===================", request);

        return true;
    }

    /**
     * 将ByteBuffer转换成能够逐行读取的BufferedReader
     * @param readBuf 从ByteBuffer转成BufferedReader
     * @return BufferedReader
     * @throws IOException out.write(readBuf.array()) 抛出的异常
     */
    private BufferedReader doByteByteBufferToBufferReader(ByteBuffer readBuf) throws IOException {

        // 将readBuf里面的内容转换成字节数组
        byte[] bytes = new byte[readBuf.remaining()];
        readBuf.get(bytes);
        // 字节数组转成输入流
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        // 转为字符输入流
        InputStreamReader isr = new InputStreamReader(input);
        // 字符流转为字符缓存
        return new BufferedReader(isr);
    }

    /**
     * 获取一段 字节转换成 str
     * @param start 起点
     * @param offers 距离起点的距离
     * @return String 这段间距获取到的str
     */
    public String getStr(int start, int offers) {
        byte[] bytes = new byte[offers]; // 临时存储
        readBuf.get(bytes,start,offers); // 获取
        return new String(bytes); // 返回
    }

    private boolean fill() throws IOException {
        boolean read = false; // 是否有数据读取
        // 从通道读取数据
        int n = socket.read(readBuf);
        // 将readBuf切换为可以读取的模式
        readBuf.flip();
        if (n == -1) {
            throw new IOException("fill() --- 通道被关闭");
        }
        if (n > 0) read = true;
        return read;
    }


    // --------------------------------------------- 解析请求参数

    private final int maxPostSize = 1024 * 1024;
    /**
     * 存储 post 数据，最大 1M
     */
    private String body;
    public void setBody(String body) { this.body = body; }

    private ByteBuffer bodyView = null; // 部分请求体数据

    /**
     * 解析 GET 和 POST 请求参数
     */
    public void readAndParseParameters() {
        // 设置已经装配好了 参数
        request.setLoadedParameters(true);
        // 1. 解析查询参数 GET
        if (request.getQuery() != null && request.getQuery().length > 0) { // 前面在解析请求行的时候，已经加入了
            parseParameters(request.getQuery()); // 将此数组里面的内容进行解析
            return;
        }

        // 2. 解析 post 请求参数并且是以键值对进行传输
        if (!"POST".contentEquals(request.getMethod())) {
            return;
        }

        // 3. 读取请求体数据
        body = null;
        try {
            // 请求体的 内容的长度
            int len = request.getContentLength();
            if (len > 0) { // identity 传输编码
                if (len > maxPostSize) { // 请全体的内容超过限制 1M
                    request.setParseParamFail(true);
                    return;
                }
                // 将body设置
                int n = readBody();
                if (n == 0) { // readBody()方法里面有调用 setBody()方法
                    // 如果设置的body参数返回数据为0，那么说明解析请求体失败
                    request.setParseParamFail(true);
                    return;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            request.setParseParamFail(true);
            return;
        }

        // 5. 解析参数
        parseParameters(body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 读取请求体数据
     * @return 因为是阻塞的，必定会返回一个大于 0 的数字，若返回 -1 连接肯定关闭了
     * @throws IOException
     */
    public int readBody() throws IOException {
        if (codec != null) { // 如果有传入 解码类，就用解码类进行 解码操作，得到
            return codec.doRead(this);
        }

        return 0;
    }

    /**
     * 从底层通道读取数据，并返回一个与结果对应视图 ByteBuffer
     *
     * @param buffHolder 持有读取数据的字节视图，如果参数值为 null，表示不需要数据，单纯的读取
     * @return 返回实际读取的数据大小，-1 表示连接被关闭
     * @throws IOException
     */
    public int realReadBytes(BufferHolder buffHolder) throws IOException {
        if (readBuf.position() >= readBuf.limit()) {
            if(!fill()) {
                buffHolder.setByteBuffer(null);
                return -1;
            }
        }
        int length = readBuf.remaining();
        if (buffHolder != null) {
            // dst 与 byteBuffer 底层共用一个 byte[]
            buffHolder.setByteBuffer(readBuf.duplicate());  // duplicate创建一个新的缓存区，存储跟原来的缓冲区存储一样的内容
        }
        readBuf.position(readBuf.limit());
        return length;
    }


    /**
     * 获取参数字符串，进行解析，并且放入request的 Parameters (HashMap<String, String>)
     * @param queryBytes 从请求行(GET)或者请求体(POST)里面获取来的
     */
    private void parseParameters(byte[] queryBytes) {
        // 得到 age=10,20&name=Tom,Tim&address=US
        String params = new String(queryBytes, request.getEncoding());
        try {
            // 将形如 "%xy" 的编码，转为正确的字符串
            params = URLDecoder.decode(params, request.getEncoding().toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 参数格式为 "a=1&b=&c=2"
        // 得到 age=10,20的集合
        String[] param = params.split("&");

        for (String key : param) {
            // age
            String name = key.substring(0, key.indexOf("="));
            // 10,20
            String values = key.substring(key.indexOf("=") + 1);
            // 放入 map
            request.getParameters().put(name, values/*.split(",")*/);
        }

    }


    /**
     * 设置解析 请求体的方法
     * @param body
     */
    public void setBodyCodec(BodyCodec body) {
        this.codec = body;
    }



    @Override
    public void setByteBuffer(ByteBuffer buffer) {
        bodyView = buffer;
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return bodyView;
    }

    @Override
    public void recycle() {
        request.recycle();

        status = ParseStatus.START;
        parsingHeader = true;
        readBuf.clear();
        body = null;
        codec = null;
        reqHeaderEndIndex = 0;
    }

}
