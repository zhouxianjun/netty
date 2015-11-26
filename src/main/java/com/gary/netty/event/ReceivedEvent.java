package com.gary.netty.event;

import com.gary.error.ErrorCode;
import com.gary.netty.codec.Worker;
import com.gary.netty.disruptor.DisruptorEvent;
import com.gary.netty.handler.Handler;
import com.gary.netty.net.Cmd;
import com.gary.netty.net.Packet;
import com.gary.netty.protobuf.ResultPro;
import com.gary.util.ClassUtil;
import com.gary.util.Utils;
import com.google.protobuf.MessageLite;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/14 15:51
 */
@Getter
@Slf4j
public abstract class ReceivedEvent<T, P extends Packet> implements Runnable, Event<T> {

    private long startTime;
    private int length;
    private short cmd;
    private ResultPro.Result ret;
    private byte[] data;
    private Worker<T, ? extends ReceivedEvent> worker;
    private Channel channel;
    private T object;

    public ReceivedEvent(int length, short cmd, T object, Channel channel, ResultPro.Result ret, Worker<T, ? extends ReceivedEvent> worker, byte[] data) {
        this.length = length;
        this.cmd = cmd;
        this.object = object;
        this.channel = channel;
        this.ret = ret;
        this.worker = worker;
        this.data = data;
        this.startTime = System.currentTimeMillis();
    }

    public void write(Packet packet) {
        if (packet.getCmd() == null)
            packet.setCmd(cmd);
        channel.writeAndFlush(packet.setStartTime(startTime));
        //log.debug("回复消息：IP:{}, 玩家:{}, CMD:0x{}, 耗时:{}毫秒", new Object[]{worker.ip, object, Integer.toHexString(cmd), System.currentTimeMillis() - startTime});
    }

    public P createSuccess(MessageLite msg) throws Exception {
        P p = Utils.getClassObject(getClass(), 1);
        return p.createSuccess(cmd, msg);
    }

    protected void handle(final HandlerEvent<Handler> handlerEvent) throws Exception {
        if(!handlerEvent.isAsync() || getDisruptorEvent() == null) {
            handle(this, handlerEvent.getHandler());
        } else {
            getDisruptorEvent().publish(new Runnable() {
                @Override
                public void run() {
                    try {
                        handle(ReceivedEvent.this, handlerEvent.getHandler());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    protected boolean checkAuthorized(Event event, Handler handler) throws Exception {
        if(handler == null) {
            event.write(((P)Utils.getClassObject(getClass(), 1)).createGlobalException(ErrorCode.NOT_FOUND));
            return false;
        }
        if (handler.getClass().getAnnotation(Cmd.class).login() && object == null){
            event.write(((P)Utils.getClassObject(getClass(), 1)).createGlobalException(ErrorCode.UN_AUTHORIZED));
            return false;
        }
        return true;
    }
    
    protected void handle(Event event, Handler handler) throws Exception {
        try {
            if (checkAuthorized(event, handler)) {
                handler.handle(event);
            }
        } catch(Exception e) {
            event.write(((P)Utils.getClassObject(getClass(), 1)).createError(ErrorCode.UNKNOWN_ERROR, null));
            log.error("Handler异常:", e);
        }
    }

    protected DisruptorEvent getDisruptorEvent(){
        return null;
    }
}
