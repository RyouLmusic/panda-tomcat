/**
 * Copyright 2019 chuonye.com - 小创编程
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ryou.tomcat.container.servletx;


import com.ryou.tomcat.http.BufferHolder;
import com.ryou.tomcat.http.RawRequest;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 实现 ServletInputStream 从底层读取请求体数据
 */
public class AppInputBuffer extends ServletInputStream implements BufferHolder {
    
    private RawRequest rawReq;
    private ByteBuffer bodyView;


    @Override
    public void setByteBuffer(ByteBuffer buffer) {

    }

    @Override
    public ByteBuffer getByteBuffer() {
        return null;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setReadListener(ReadListener readListener) {

    }

    @Override
    public int read() throws IOException {
        return 0;
    }
}
