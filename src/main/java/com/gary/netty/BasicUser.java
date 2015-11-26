package com.gary.netty;

import com.gary.netty.codec.Worker;
import com.gary.netty.listeners.UserStateListener;
import com.gary.netty.net.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/2 10:07
 */
@Slf4j
public abstract class BasicUser<S> {
    public String id;

    public String name;

    public int sex;

    public S server;

    public volatile Channel channel;

    /**
     * 登陆时间（秒）
     */
    public volatile int loginTime;
    /**
     * 心跳时间
     */
    public volatile long heartTime;

    /**
     * 最后离线时间（秒）
     */
    public volatile long lastOfflineTime;

    public volatile Worker worker;

    /**
     * 给玩家发数据包
     * @param packet
     */
    public ChannelFuture send(Packet packet) {
        if(isOnline()) {
            return channel.writeAndFlush(packet);
        }
        return null;
    }
    /**
     * 判断玩家是否在线
     * @return
     */
    public boolean isOnline() {
        if(channel == null) return false;
        if(!channel.isActive()) {
            return false;
        }
        return true;
    }

    public void offline() {
        channel.close();
        stateChanged("offline");
    }

    public void reconnect() {
        stateChanged("reconnect");
    }

    public void online() {
        stateChanged("online");
    }

    /**
     * 玩家状态变更
     * @param state
     */
    private void stateChanged(final String state){
        List<? extends UserStateListener> userStateListeners = getUserStateListeners();
        if (userStateListeners == null || userStateListeners.isEmpty())
            return;
        Iterator<? extends UserStateListener> it = userStateListeners.iterator();
        while(it.hasNext()) {
            final UserStateListener listener = it.next();
            worker.executeTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        UserStateListener.class.getMethod(state, BasicUser.class).invoke(listener, BasicUser.this);
                    } catch (Exception e) {
                        log.warn("用户:[{}]状态变更:<{}>异常:{}", new Object[]{BasicUser.this, state, e});
                        log.warn("", e);
                    }
                }
            });
        }
    }

    protected abstract List<? extends UserStateListener> getUserStateListeners();
}
