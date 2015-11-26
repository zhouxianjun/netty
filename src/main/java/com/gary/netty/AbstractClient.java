package com.gary.netty;

import com.gary.netty.codec.BasicEncoderHandler;
import com.gary.netty.net.Packet;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/15 15:56
 */
@Slf4j
public abstract class AbstractClient {
    protected String ip;
    protected int port;
    @Setter
    protected Boolean reconnect = false;
    protected Integer reconnectCount = 3;
    protected String name;
    protected Integer readTimeOut = 60;
    protected Integer writerTimeOut = 60;
    protected boolean stateChange = false;
    private Bootstrap bootstrap;
    protected Channel channel;
    private ScheduledExecutorService scheduled;
    private ChannelGroup allChannels;

    public AbstractClient(String ip, int port, String name) {
        this.ip = ip;
        this.port = port;
        this.name = name;
        init();
    }

    private void init(){
        EventLoopGroup workerGroup = new NioEventLoopGroup(2,
                DefaultThreadFactory.newThreadFactory(getName() + "[ip=" + ip + ",port=" + port + "]_THREAD_"));

        try {
            bootstrap = new Bootstrap();

            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.MAX_MESSAGES_PER_READ, 4096)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline channelPipeline = ch.pipeline();
                            channelPipeline.addLast(getDecoderHandler()).addLast(getEncoderHandler());
                            if (stateChange){
                                channelPipeline.addLast("idleStateHandler", new IdleStateHandler(readTimeOut, writerTimeOut, 0));
                            }
                        }
                    });
            log.info("初始化{}连接客户端【IP={},Port={}】", new Object[]{getName(), ip, port});
        } catch (Exception e) {
            log.error("客户端初始化异常", e);
        }
    }

    public void connect(){
        if (reconnect && scheduled == null){
            scheduled = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

                @Override
                public Thread newThread(Runnable arg0) {
                    Thread thread = new Thread(arg0);
                    thread.setName(getName() + "_CLIENT_TASK");
                    return thread;
                }
            });
        }
        try {
            bootstrap.connect(ip, port).addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        log.info("连接上服务器【IP={},Port={}】", ip, port);
                        channel = future.channel();
                        connected(channel);
                    } else {
                        if (reconnect && scheduled != null) {
                            scheduled.schedule(new Runnable() {

                                @Override
                                public void run() {
                                    log.info("开始重连服务器【IP={},PORT={}】", ip, port);
                                    connect();
                                }
                            }, 3, TimeUnit.SECONDS);
                        } else {
                            log.warn("连接服务器【IP={},Port={}】失败!", ip, port);
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("连接服务器异常", e);
        }
    }

    protected abstract void connected(Channel channel);

    protected String getName(){
        this.name =  this.name == null ? "Client-" + UUID.randomUUID().toString() : this.name;
        return this.name;
    }

    public boolean isAvailable() {
        if(channel == null || !channel.isActive()) {
            return false;
        }
        return true;
    }

    public void writer(Object o){
        channel.writeAndFlush(o);
    }

    public void shutdown() {
        if(channel != null) {
            log.info("关闭与服务器【ip={}，port={}】的连接", ip, port);
            channel.close();
        }
    }

    public ChannelGroup getAllChannels(){
        if (allChannels == null){
            allChannels = new DefaultChannelGroup(new DefaultEventExecutorGroup(1).next());
        }
        return allChannels;
    }
    protected abstract ChannelHandler getDecoderHandler();
    protected ChannelHandler getEncoderHandler(){
        return new BasicEncoderHandler();
    }
}
