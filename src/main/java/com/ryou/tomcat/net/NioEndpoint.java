package com.ryou.tomcat.net;

import ch.qos.logback.classic.Logger;
import com.ryou.tomcat.http.HttpNioProcessor;
import com.ryou.tomcat.http.HttpNioProcessorTemporary;
import com.ryou.tomcat.net.util.SocketState;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 汉高鼠刘邦
 * @Date: 2021/3/7 12:09
 */
public class NioEndpoint {
    // 日志
    final static Logger log = (Logger) LoggerFactory.getLogger(NioEndpoint.class);
    /**
     * endPoint的运行状态、
     */
    private volatile boolean running = false;
    /**
     * endPoint是否初始化
     */
    protected boolean initialized = false;
    /**
     * 管理SocketChannel的连接
     */
    private Acceptor acceptor;
    /**
     * 轮询
     */
    protected Poller[] pollers = null;
    protected int pollerRoundRobin = 0;
    public Poller getPoller0() {
        pollerRoundRobin = (pollerRoundRobin + 1) % pollers.length;
        return pollers[pollerRoundRobin];
    }
    // TODO 可以修改
    int pollerThreadCount = 3;
    /**
     * 服务器的连接
     */
    private ServerSocketChannel serverSocket;
    /**
     * 端口，可以通过配置修改
     */
    private int port = 8888;
    public void setPort(int port) {
        this.port = port;
    }
    /**
     * 获取 设置的端口号
     * @return
     */
    public int getPort() {
        return port;
    }


    /**
     * socket的读取read方法阻塞的超时时间，TODO 也可以通过配置修改
     */
    private final int soTimeout = 20000; // 20s
    /**
     * threadPriority: 线程的优先级别，TODO
     */
    private final int threadPriority = Thread.NORM_PRIORITY; // 5

    /** 已完成 3 次握手，还没有被应用层接收的连接队列大小 */
    private final int acceptCount = 100;// backlog

    private Handler handler;

    /* 线程池 */
    private ExecutorService executor;
    // TODO 配置
    private final int maxThreads = 25;
    private final int corePoolSize = 10;

    /**
     * 初始化NioEndPoint，被connector调用
     */
    public void init() throws IOException {
        // 开启服务线程
        serverSocket = ServerSocketChannel.open();
        // 绑定端口号
        serverSocket.bind(new InetSocketAddress(port), acceptCount);
        // 此处设置为阻塞的，这样接下来的serverSocket.accept方法就是阻塞的，没有请求会在此方法阻塞住
        serverSocket.configureBlocking(true);
        // 设置超时时间。。。TODO
        //  当调用socket.getInputStream().read()方法时,由于这个read()方法是阻塞的,read()方法会一直处于阻塞状态等待接受数据而导致不能往下执行代码;而setSoTimeout()方法就是设置阻塞的超时时间.
        //当设置了超时时间后,如果read()方法读不到数据,处于等待读取数据的状态时,就会开始计算超时时间;当到达超时时间还没有新的数据可以读取的时候,read()方法就会抛出io异常,结束read()方法的阻塞状态;如果到达超时时间前,从缓冲区读取到了数据,那么就重新计算超时时间.
        serverSocket.socket().setSoTimeout(soTimeout);

        // 已经初始化完成
        initialized = true;
    }


    /**
     * Start the NIO endpoint, creating acceptor, poller threads
     * 被connector调用
     */
    public void startInternal() {

        // 如果还未进行初始化，进进行初始化
        if (!initialized) {
            try {
                init();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 没有在运行,即没有被其他线程所占用
        if (!running) {
            // 设置状态是在运行
            running = true;

            // 创建线程池
            if ( getExecutor() == null ) {
                createExecutor();
            }

            // 初始化并启动 Poller 和 Acceptor 线程
            // 开启轮训者线程
            pollers = new Poller[pollerThreadCount];// pollerThreadCount:2
            startPollerThread();
            // 开启接收者线程
            startAcceptorThread();

        }

    }

    /**
     * 开启轮训者线程
     */
    private void startPollerThread() {
        for (int i = 0; i < pollers.length; i++) {
            try {
                pollers[i] = new Poller(this);
            } catch (IOException e) {
                e.printStackTrace();
                log.info("Poller开启失败--" + i);
            }
            Thread pollerThread = new Thread(pollers[i],  getPort() + "-ClientPoller-"+i);
            pollerThread.setPriority(threadPriority);
            // 设置为守护线程：如果所有的用户线程都关闭，那么守护线程会直接关闭
            pollerThread.setDaemon(true);
            // 开启线程
            pollerThread.start();
        }
    }

    /**
     * 开启接收者线程 管理socketChannel.accept
     */
    private void startAcceptorThread() {
        // 实例化Acceptor
        acceptor = new Acceptor(this);
        // 启动线程
        Thread acceptorThread = new Thread(acceptor, getPort() + "-Acceptor-0");
        // 设置优先级
        acceptorThread.setPriority(threadPriority);
        // 启动
        acceptorThread.start();

    }


    /**
     * 返回线程池 ，此线程池是来 处理请求和回复的
     * @return ExecutorService
     */
    private ExecutorService getExecutor() {
        return executor;
    }


    /**
     * 线程池的创建，初始化
     */
    private void createExecutor() {
        executor = new ThreadPoolExecutor(
                corePoolSize,maxThreads,
                500,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }


    /*-----------------------------------------给Acceptor和Poller调用的方法*/
    /**
     * 返回运行状态
     * @return boolean
     */
    public boolean isRunning() {
        return running;
    }


    /**
     * endpoint关闭,
     * 是上面的servlet调用其生命周期的destroy方法的的时候才会调用到此方法
     */
    public void stop()  {
        running = false;
        try {
            for (Poller poller : pollers) {
                poller.destroy();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        pollers = null;
        // 关闭线程池
        executor.shutdownNow();
    }

    /**
     * 进行获取 客户端的请求连接
     * @return
     * @throws IOException
     */
    public SocketChannel accept() throws IOException {
        return serverSocket.accept();
    }

    /**
     * 给接收的请求从处理操作 进行设置
     * @param handler 请求处理操作器Handler
     */
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public Handler getHandler() {
        return handler;
    }

    /**
     * 获取超时时间
     * @return
     */
    public int getSoTimeout() {
        return soTimeout;
    }


    /**
     * 此方法被 poller的run方法所调用
     * 将事件交由线程池处理
     * @param socket NioChannel
     * @return boolean,操作是否成功
     */
    protected boolean processSocket(NioChannel socket) {
        try {
            if (executor == null) { // 如果线程池是空
                createExecutor();
            } else {
                // 设置通道的超时时间的起始时间量  这里是最新的，这样 超过界限（此处是起点）才会关闭通道
                socket.access();
                executor.execute(new SocketProcessor(socket));
            }
        } catch (Throwable t) {
            // This means we got an OOM or similar creating a thread, or that
            // the pool and its queue are full
            log.error("endpoint.process.fail", t);
            return false;
        }
        return true;
    }




    /*------------------------------------------SocketProcessor*/

    /**
     * This class is the equivalent of the Worker, but will simply use in an
     * external Executor thread pool.
     *
     * 线程池里面运行的类class
     */
    protected class SocketProcessor implements Runnable {

        protected NioChannel socket;

        public SocketProcessor(NioChannel socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            // process(socket)真正的处理，进行读和写
            /** handler是可以根据 setHandle进行真正的设置的 */
            if (handler.process(socket) == SocketState.CLOSED) {
                // 在Handler类里面的操作失败之后，进行通道的关闭，并且将通道置为空，可以被回收
                try {
                    try {socket.close();} catch (Exception ignore){}
                    // socket不为空并且是打开的，就进行关闭操作
                    if (socket != null && socket.isOpen()) {
                        socket.close(true);
                        // 将映射关系也释放掉
                        handler.release(socket);
                    }
                } catch ( Exception x ) {
                    log.error("",x);
                }
                // 通道置为空
                socket = null;
            }

        }

    }


    public static void main(String[] args) {
        NioEndpoint endPoint = new NioEndpoint();
        endPoint.setHandler(new Handler() {
            @Override
            protected Processor createProcessor() {
                return new HttpNioProcessorTemporary();
            }
        });
        endPoint.startInternal();
    }

}
