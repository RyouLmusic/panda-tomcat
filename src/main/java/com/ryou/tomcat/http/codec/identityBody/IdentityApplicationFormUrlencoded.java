package com.ryou.tomcat.http.codec.identityBody;

import com.ryou.tomcat.http.codec.ParsesRequestBody;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/27 14:45
 *
 * application/x-www-form-urlencoded 格式解析方式
 *
 * Content-Type:application/x-www-form-urlencoded
 *
 * ss=ssd&hjg=678&ds=r&rer=ert&ter=re&t=tt%2Cdd&e=er&rt=er
 *
 */
public class IdentityApplicationFormUrlencoded implements ParsesRequestBody {


    public IdentityApplicationFormUrlencoded() {
    }

    /**
     *
     * @param bufferedReader 请求头数据缓存区
     * @param contentLength 请全体长度
     * @return ss=ssd&hjg=678&ds=r&rer=ert&ter=re&t=tt%2Cdd&e=er&rt=er 这种格式的请求参数
     * @throws IOException 读取的时候出现错误
     */
    @Override
    public String decodeMethod(BufferedReader bufferedReader, int contentLength) throws IOException {
        char[] chars = new char[contentLength];
        bufferedReader.read(chars);

        return new String(chars);
    }
}
