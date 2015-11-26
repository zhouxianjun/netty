package com.gary.netty.codec;

import com.gary.netty.net.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:发送给玩家的消息编码
 * @date 2015/6/4 16:36
 */
@Slf4j
public class BasicEncoderHandler extends MessageToByteEncoder<Packet> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) throws Exception {
        msg.write(out);
        String ip = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        if (Packet.PING.getCmd().shortValue() != msg.getCmd().shortValue() && msg.isPrint()){
            Attribute attr = ctx.channel().attr(Worker.PLAYER_KEY);
            long start = msg.getStartTime();
            log.debug("回复消息：IP:{}, 对象:{}, CMD:0x{}, code:{}, msg: {}, 总大小:{}, ret大小:{}, body大小:{}, 耗时:{}毫秒", new Object[]{ip, attr.toString(), Integer.toHexString(msg.getCmd()), msg.getRet().getCode(), msg.getRet().getMsg(), msg.calcSize(), msg.getRet().getSerializedSize(), msg.getBodyLength(), start > 0 ? System.currentTimeMillis() - start : null});
        }
    }
}
