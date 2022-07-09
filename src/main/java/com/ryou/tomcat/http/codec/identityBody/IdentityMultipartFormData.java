package com.ryou.tomcat.http.codec.identityBody;

import com.ryou.tomcat.http.codec.ParsesRequestBody;
import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/27 14:43
 *
 * TODO 此处主要是进行 文件上传的，还未实现
 *
 * Content-Type:multipart/form-data 格式的解析
 *
 * Content-Type: multipart/form-data; boundary=--------------------------697105616901889664132475
 *
 * ----------------------------697105616901889664132475
 * Content-Disposition: form-data; name="hbb"
 *
 * ddd
 * ----------------------------697105616901889664132475
 * Content-Disposition: form-data; name="fgg"
 *
 * gdgdr
 * ----------------------------697105616901889664132475--
 */
public class IdentityMultipartFormData implements ParsesRequestBody {

    private String boundary;

    public IdentityMultipartFormData(String boundary) {
        this.boundary = boundary;
    }

    @Override
    public String decodeMethod(BufferedReader bufferedReader, int contentLength) throws IOException {

        char[] chars = new char[contentLength];

        bufferedReader.read(chars);

        String content = new String(chars);
        if (content.contains("Content-Type:")) {
            // TODO 现在只支持Text方式
            throw new DecodingException("此解析方式 现在只支持Text");
        }
        String[] split = content.split(boundary);
        for (String str : split) {

            if (str.trim().equals("")) continue;
            System.out.println("===========" + str);
//            String paramName = str.substring(str.indexOf("name="));
//            String paramValue = str.substring(str.indexOf("\n"));


//            System.out.println(paramName + ":   " + paramValue);
        }

        StringBuilder reStr = new StringBuilder();
        return new String(reStr);
    }
}
