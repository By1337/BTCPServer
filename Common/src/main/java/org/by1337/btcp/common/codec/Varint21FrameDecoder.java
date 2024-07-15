package org.by1337.btcp.common.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import org.by1337.btcp.common.io.ByteBuffer;

import java.util.List;

public class Varint21FrameDecoder extends ByteToMessageDecoder {

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        in.markReaderIndex();
        byte[] bytes = new byte[3];

        for(int i = 0; i < bytes.length; ++i) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return;
            }

            bytes[i] = in.readByte();
            if (bytes[i] >= 0) {
                ByteBuffer byteBuf = new ByteBuffer(Unpooled.wrappedBuffer(bytes));

                try {
                    int len = byteBuf.readVarInt();
                    if (in.readableBytes() < len) {
                        in.resetReaderIndex();
                        return;
                    }

                    out.add(in.readBytes(len));
                } finally {
                    byteBuf.release();
                }

                return;
            }
        }

        throw new CorruptedFrameException("length wider than 21-bit");
    }
}
