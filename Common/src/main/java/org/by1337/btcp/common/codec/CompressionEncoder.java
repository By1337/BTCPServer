package org.by1337.btcp.common.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.io.zip.ZLibCompressor;
import org.by1337.btcp.common.io.zip.ZLibFactory;


public class CompressionEncoder extends MessageToByteEncoder<ByteBuf> {
    private final int threshold;
    private final ZLibCompressor compressor;


    public CompressionEncoder(int threshold, int lvl) {
        this.threshold = threshold;
        compressor = ZLibFactory.createCompressor(lvl);
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf in, ByteBuf out) throws Exception {
        int size = in.readableBytes();
        ByteBuffer outWrapped = new ByteBuffer(out);
        if (size < this.threshold) {
            outWrapped.writeVarInt(0);
            out.writeBytes(in);
        } else {
            outWrapped.writeVarInt(size);
            compressor.compress(in, out);
            in.skipBytes(in.readableBytes());
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        release();
        super.handlerRemoved(ctx);
    }

    public void release() {
        compressor.release();
    }
}
