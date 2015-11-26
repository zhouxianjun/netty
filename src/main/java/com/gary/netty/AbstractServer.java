package com.gary.netty;

import com.gary.netty.codec.BasicEncoderHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/16 14:58
 */
@Slf4j
public abstract class AbstractServer {
    private ServerBootstrap serverBootstrap;
    protected int port;
    private ChannelGroup allChannels;
    protected Integer readTimeOut(){
        return 60;
    }
    protected Integer writerTimeOut(){
        return 60;
    }
    protected boolean stateChange(){return false;}
    public AbstractServer(int boss, int worker) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(boss,
                DefaultThreadFactory.newThreadFactory("NETTY_BOSS_THREAD_"));
        EventLoopGroup workerGroup = new NioEventLoopGroup(worker,
                DefaultThreadFactory.newThreadFactory("NETTY_WORKER_THREAD_"));

        try {
            serverBootstrap = new ServerBootstrap();
            final boolean stateChange = this.stateChange();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_RCVBUF, 2048)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            if (stateChange){
                                pipeline.addLast(new IdleStateHandler(readTimeOut(), writerTimeOut(), 60));
                            }
                            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
                            pipeline.addLast(getDecoderHandler());
                            pipeline.addLast(getEncoderHandler());
                        }
                    });
        } catch (Exception e) {
            log.error("创建服务器异常!", e);
        }
    }
    public boolean start(int port){
        try {
            getAllChannels().add(serverBootstrap.bind(port).channel());
            log.info("服务器启动成功，开始监听{} 端口...", port);
            return true;
        } catch (Exception e) {
            log.error("启动服务器异常!", e);
        }
        return false;
    }
    protected ChannelGroup getAllChannels(){
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
