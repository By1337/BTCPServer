package org.by1337.btcp.common.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.io.zip.ZLibDecompressor;
import org.by1337.btcp.common.io.zip.ZLibFactory;

import java.util.List;

public class CompressionDecoder extends ByteToMessageDecoder {
    private final int threshold;
    private final ZLibDecompressor decompressor;

    public CompressionDecoder(int threshold) {
        this.threshold = threshold;
        decompressor = ZLibFactory.createDecompressor();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() != 0) {
            ByteBuffer inWrapped = new ByteBuffer(in);
            int originalSize = inWrapped.readVarInt();
            if (originalSize == 0) {
                out.add(in.readBytes(in.readableBytes()));
            } else {
                if (originalSize < this.threshold) {
                    throw new DecoderException("Badly compressed packet - size of " + originalSize + " is below server threshold of " + this.threshold);
                }
                ByteBuf result = Unpooled.directBuffer();
                decompressor.decompress(in, result, originalSize);
                out.add(result);
                in.skipBytes(in.readableBytes());
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        release();
        super.channelInactive(ctx);
    }

    public void release() {
        decompressor.release();
    }
}
