package org.by1337.btcp.common.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.by1337.btcp.common.io.ByteBuffer;

public class Varint21LengthFieldPrepender extends MessageToByteEncoder<ByteBuf> {
    private static final int MAX_BYTES = 3;
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        int readableBytes = msg.readableBytes();
        int var21size = ByteBuffer.getVarIntSize(readableBytes);
        if (var21size > MAX_BYTES) {
            throw new IllegalArgumentException("unable to fit " + readableBytes + " into 3");
        } else {
            ByteBuffer byteBuf = new ByteBuffer(out);
            byteBuf.ensureWritable(var21size + readableBytes);
            byteBuf.writeVarInt(readableBytes);
            byteBuf.writeBytes(msg, msg.readerIndex(), readableBytes);
        }
    }
}
