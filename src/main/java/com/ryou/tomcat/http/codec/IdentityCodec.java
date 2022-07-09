package com.ryou.tomcat.http.codec;

import com.ryou.tomcat.http.BodyCodec;
import com.ryou.tomcat.http.InputBuffer;
import com.ryou.tomcat.http.OutputBuffer;
import com.ryou.tomcat.http.codec.identityBody.IdentityApplicationFormUrlencoded;
import com.ryou.tomcat.http.codec.identityBody.IdentityApplicationJson;
import com.ryou.tomcat.http.codec.identityBody.IdentityMultipartFormData;
import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/13 17:06
 *
 */
public class IdentityCodec extends BodyDecodeContext implements BodyCodec {
    private int contentLength = -1; // 总长度
    private int remaining; // 剩余字节数

    private final String contentType;

    public IdentityCodec(int length, String contentType) {
        contentLength = length;
        remaining = length;

        this.contentType = contentType;
    }

    @Override
    public int doRead(InputBuffer input) throws IOException {
        int result = -1;


        BufferedReader bufferedReader = input.getBufferedReader();

        String params = null;
        // 按照contentType进行分配解析方式
        if (contentType.equalsIgnoreCase(BodyForm.X_WWW_FORM_URLENCODED.getForm())) {
            setStrategy(new IdentityApplicationFormUrlencoded());
            // 从请求体获取的 参数 ：格式为  ss=ssd&hjg=678&ds=r&
            params = decodeMethod(bufferedReader, contentLength);
        }
        else if (contentType.equalsIgnoreCase(BodyForm.APPLICATION_JSON.getForm())) {
            setStrategy(new IdentityApplicationJson());
            params = decodeMethod(bufferedReader, contentLength);

            throw new DecodingException("json格式的还未实现");
        }
        else {
            String[] split = contentType.split("; ");
            String str = "boundary=";
            if (split.length == 2 && split[0].equalsIgnoreCase(BodyForm.FORM_DATA.getForm()) && split[1].startsWith(str)) {
                String boundary = split[1].replace(str, "");
                // 实例化解析策略
                setStrategy(new IdentityMultipartFormData(boundary));
                // 调用解析方法
                params = decodeMethod(bufferedReader, contentLength);

                throw new DecodingException("还未实现FORM_DATA");

            }
            else {
                // 如果请求体的格式不是上面三种中的一种，就抛出异常
                throw new DecodingException("请求体的格式无法进行解析......");
            }
        }

        if (params != null){
//            input.setBody(params);
            // 设置参数的数据成功，返回参数的长度
            result = params.length();
        }


        return result;
    }

    /**
     * TODO  ????
     * @param input 关联的 HTTP 请求解析类
     * @throws IOException
     */
    @Override
    public void endRead(InputBuffer input) throws IOException {

    }

    @Override
    public void doWrite(OutputBuffer output, ByteBuffer src) throws IOException {
        // 定长写入比较简单，直接写就行
        output.write(src);
    }

    @Override
    public void endWrite(OutputBuffer output) throws IOException {
    }
}
