package com.gary.netty.net;

import com.gary.error.ErrorCode;
import com.gary.netty.protobuf.EmptyPro;
import com.gary.netty.protobuf.ResultPro;
import com.gary.util.ErrorsUtil;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2015/3/6 10:29
 */
@Slf4j
@Getter
public class Packet {
    @Setter
    private Short cmd;

    private ResultPro.Result ret;

    private MessageLite body;

    private long startTime;

    @Setter
    private byte[] bodyData;

    @Setter
    private boolean print = true;

    public ResultPro.Result getRet(){
        if (ret == null) {
            ret = ResultPro.Result.getDefaultInstance();
        }
        if (ret.getCode() != ret.getDefaultInstanceForType().getCode()) {
            try {
                ResultPro.Result.Builder builder = ResultPro.Result.parseFrom(ret.toByteArray()).toBuilder();
                if (!builder.hasMsg()) {
                    builder.setMsg(ErrorsUtil.getErrorDesc(builder.getCode()));
                    return builder.build();
                }
            } catch (InvalidProtocolBufferException e) {
                log.error("获取ret异常!", e);
            }
        }
        return ret;
    }

    public MessageLite getBody(){
        if (body == null)
            body = EmptyPro.Empty.getDefaultInstance();
        return body;
    }

    public int getBodyLength(){
        return getBodyData() == null ? getBody().getSerializedSize() : getBodyData().length;
    }

    public <T extends Packet> T createException(Short cmd, int errorCode, MessageLite body){
        ResultPro.Result.Builder result = ResultPro.Result.newBuilder();
        result.setCode(errorCode);
        this.setCmd(cmd);
        this.ret = result.build();
        this.body = body;
        this.startTime = 0;
        this.bodyData = null;
        return (T) this;
    }

    public <T extends Packet> T createGlobalException(){
        return createException(Cmd.GLOBAL_EXCEPTION, ErrorCode.UNKNOWN_ERROR, null);
    }

    public <T extends Packet> T createGlobalException(int errorCode){
        return createException(Cmd.GLOBAL_EXCEPTION, errorCode, null);
    }

    public <T extends Packet> T createSuccess(Short cmd, MessageLite body){
        this.cmd = cmd;
        this.ret = null;
        this.body = body;
        this.startTime = 0;
        this.bodyData = null;
        return (T) this;
    }
    public <T extends Packet> T createSuccess(MessageLite body){
        return createSuccess(null, body);
    }
    public <T extends Packet> T createSuccess(){
        return createSuccess(null);
    }

    public <T extends Packet> T createError(int errorCode, MessageLite body){
        return createException(null, errorCode, body);
    }
    public <T extends Packet> T createError(int errorCode){
        return createException(null, errorCode, null);
    }

    public int calcSize(){
        int size = 4; //cmd
        size += getRet().toByteArray().length;
        if(getBodyData() != null) {
            size += getBodyData().length;
        }else if (getBody() != null){
            size += getBody().toByteArray().length;
        }
        return size;
    }

    public void write(ByteBuf byteBuf){
        byteBuf.writeShort(calcSize()); //输出总长度
        byteBuf.writeShort(cmd); //命令
        byteBuf.writeShort(getRet().toByteArray().length); //命令
        byteBuf.writeBytes(getRet().toByteArray());

        if(getBodyData() != null) {
            byteBuf.writeBytes(getBodyData());
        }else if (getBody() != null){
            byteBuf.writeBytes(getBody().toByteArray());
        }
    }

    public Packet setStartTime(long startTime){
        this.startTime = startTime;
        return this;
    }

    public int readLength(ByteBuf buffer){
        return buffer.readShort();
    }

    public static final Packet PING = new Packet().createSuccess(Cmd.PING, null);
}
