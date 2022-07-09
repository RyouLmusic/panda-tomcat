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
package com.ryou.tomcat.container.core;

import com.ryou.tomcat.container.core.context.Context;
import com.ryou.tomcat.http.Adapter;
import com.ryou.tomcat.http.HttpNioProcessor;
import com.ryou.tomcat.net.Handler;
import com.ryou.tomcat.net.NioEndpoint;
import com.ryou.tomcat.net.Processor;

import java.io.IOException;

/**
 * 连接器，它主要提供 xml 配置功能，比如指定端口，超时时间等
 * 
 * @author
 */
public class Connector {

	private Context context;

    private int port = 10393;

	// Endpoint
	private NioEndpoint endpoint;
	// Adapter
	private Adapter adapter;

	public Connector() {
        adapter = new AdapterImpl(this);
        endpoint = new NioEndpoint(); // 实例化endpoint
        // 设置处理请求和响应的操作
        endpoint.setHandler(new Handler() {
            @Override
            public Processor createProcessor() {
                HttpNioProcessor processor = new HttpNioProcessor();
                // 设置processor里面的适配器，让processor可以调用service方法
                processor.setAdaptor(adapter);
                return processor;
            }
        });
    }

    /**
     * endpoint进行初始化
     * @throws IOException IO异常
     */
	public void start() throws IOException{
        endpoint.init();
        endpoint.startInternal();
	}

	public void stop() {
	    endpoint.stop();
	}

	// Getter & Setter
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
		endpoint.setPort(port);
	}


	public void setContext(Context cxt) {
	    context = cxt;
	}

    public Context getContainer() {
        return context;
    }
}
