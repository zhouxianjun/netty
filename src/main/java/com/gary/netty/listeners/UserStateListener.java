package com.gary.netty.listeners;

import com.gary.netty.BasicUser;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/4/2 10:12
 */
public interface UserStateListener<T extends BasicUser> {
    /**
     * 离线
     * @param user
     */
    void offline(T user);
    /**
     * 上线
     * @param user
     */
    void online(T user);
    /**
     * 重连
     * @param user
     */
    void reconnect(T user);
}
