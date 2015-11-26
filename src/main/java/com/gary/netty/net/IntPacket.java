package com.gary.netty.net;

import io.netty.buffer.ByteBuf;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/7/8 19:43
 */
public class IntPacket extends Packet {

    @Override
    public void write(ByteBuf byteBuf) {
        byteBuf.writeInt(calcSize()); //输出总长度
        byteBuf.writeShort(getCmd()); //命令
        byteBuf.writeShort(getRet().toByteArray().length); //命令
        byteBuf.writeBytes(getRet().toByteArray());

        if(getBodyData() != null) {
            byteBuf.writeBytes(getBodyData());
        }else if (getBody() != null){
            byteBuf.writeBytes(getBody().toByteArray());
        }
    }

    @Override
    public int readLength(ByteBuf buffer) {
        return buffer.readInt();
    }
}
