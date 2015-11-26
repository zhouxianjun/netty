package com.gary.netty.codec;

import com.gary.netty.event.ReceivedEvent;
import com.gary.netty.net.Cmd;
import com.gary.netty.net.Packet;
import com.gary.netty.protobuf.ResultPro;
import com.gary.util.ClassUtil;
import com.gary.util.Utils;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/14 11:26
 */
@Slf4j
public abstract class AbstractDecoderHandler<W extends Worker<T, ? extends ReceivedEvent<T, P>>, T, P extends Packet> extends LengthFieldBasedFrameDecoder {
    private final ChannelGroup channelGroup;
    private Channel channel;
    protected Worker<T, ? extends ReceivedEvent<T, P>> worker;
    @Setter
    private Class workerClass;
    @Setter
    private Class packetClass;
    private P packageObject;
    public AbstractDecoderHandler(ChannelGroup channelGroup) {
        super(4096, 0, 2, 0, 0);
        this.channelGroup = channelGroup;
    }

    public AbstractDecoderHandler(ChannelGroup channelGroup, int maxFrameLength,
                                  int lengthFieldOffset, int lengthFieldLength,
                                  int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
        this.channelGroup = channelGroup;
    }

    @Override
    protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
        try {
            if(buffer.readableBytes() < 1){
                log.warn("message length error......");
                channel.close();
                return null;
            }
            //整个消息包大小
            Class<? extends P> eventClass = getPacketClass();
            P p = getPackageObject(eventClass);
            if (p == null){
                log.error("获取包对象异常!Class:{}", eventClass);
                return null;
            }
            final int totalLength = p.readLength(buffer);
            //当前请求CMD
            final short cmd = buffer.readShort();
            //消息RET 长度
            final short retSize = buffer.readShort();
            //消息RET
            final byte[] ret = new byte[retSize];
            buffer.readBytes(ret);
            ResultPro.Result result;
            try {
                result = ResultPro.Result.parseFrom(ret);
            } catch (InvalidProtocolBufferException e) {
                result = ResultPro.Result.getDefaultInstance();
            }
            //消息body
            int bodyLen = totalLength - 4 - retSize;
            int readableBytes = buffer.readableBytes();
            final byte[] body = new byte[bodyLen];
            buffer.readBytes(body);
            if (cmd != Cmd.PING && worker.printMsg(cmd)) {
                log.info("接收到消息:CMD:0x{}, ip:{}, 总长度:{}-{}, ret长度:{}, body长度:{}, code:{}, msg:{}", new Object[]{Integer.toHexString(cmd), ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress(), totalLength, length, retSize, body.length, result.getCode(), result.getMsg()});
            }
            if (readableBytes != bodyLen){
                log.warn("消息0x{}, body异常,表示长度:{},实际长度:{}, 接收长度:{}", new Object[]{Integer.toHexString(cmd), bodyLen, readableBytes, length});
            }
            messageReceived(buffer, totalLength, cmd, result, body);
        } catch (Exception e) {
            log.error("IP:"+ worker.ip +" 接收消息异常:不符合标准!", e);
        }
        return null;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelGroup.add(channel);
        connection(channel);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        disconnection();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        log.error("IP:" + worker.ip + " 连接异常，关闭连接", cause);
        ctx.close().sync();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            timeOut(e.state(), ctx);
        }
    }

    /**
     * 收到消息
     * @param buffer
     * @param length
     * @param cmd
     * @param result
     * @param body
     */
    protected void messageReceived(ByteBuf buffer, int length, short cmd, ResultPro.Result result, byte[] body){
        worker.messageReceived(buffer, length, cmd, result, body);
    }
    protected void connection(Channel channel){
        Class<? extends Worker<T, ? extends ReceivedEvent<T, P>>> eventClass = getWorkerClass();
        try {
            Constructor<? extends Worker<T, ? extends ReceivedEvent<T, P>>> constructor = eventClass.getDeclaredConstructor(new Class[]{Channel.class});
            worker = constructor.newInstance(channel);
        } catch (Exception e) {
            log.error("工作创建失败!", e);
        }
    }
    protected Class<? extends Worker<T, ? extends ReceivedEvent<T, P>>> getWorkerClass(){
       return workerClass == null ? (Class<? extends Worker<T, ? extends ReceivedEvent<T, P>>>) ClassUtil.getSuperClassGenricType(getClass(), 0) : workerClass;
    }
    protected Class<? extends P> getPacketClass(){
       return packetClass == null ? (Class<? extends P>) ClassUtil.getSuperClassGenricType(getClass(), 2) : packetClass;
    }
    /**
     * 断开连接
     */
    protected void disconnection(){
        if (worker != null)
            worker.processDisconnection();
    }

    protected P getPackageObject(Class<? extends P> eventClass){
        if (packageObject == null) {
            try {
                Constructor<? extends P> constructor = eventClass.getDeclaredConstructor();
                packageObject = constructor.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return packageObject;
    }

    /**
     * 超时
     * @param state
     * @param ctx
     * @throws Exception
     */
    protected void timeOut(IdleState state, ChannelHandlerContext ctx) throws Exception {
        log.warn("IP:{}, {} 超时，关闭连接", worker.ip, state.name());
        ctx.close().sync();
    }
}
