package org.by1337.btcp.common.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.packet.PacketRegistry;
import org.by1337.btcp.common.packet.PacketType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class PacketDecoder extends ByteToMessageDecoder {
    private static final Marker MARKER = MarkerFactory.getMarker("[PACKET_RECEIVED]");
    private final static Logger LOGGER = LoggerFactory.getLogger(PacketDecoder.class);
    private final boolean debug;
    private final PacketFlow packetFlow;

    public PacketDecoder(boolean debug, PacketFlow packetFlow) {
        this.debug = debug;
        this.packetFlow = packetFlow;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() == 0) return;

        list.add(read(new ByteBuffer(byteBuf)));
        if (byteBuf.readableBytes() > 0) {
            throw new DecoderException("bad packet!");
        }
    }

    private Packet read(ByteBuffer byteBuf) throws IOException {
        Packet packet = byteBuf.readPacket();
        PacketType<?> type = PacketRegistry.get().lookup(packet.getClass());

        if (type.getFlow() != PacketFlow.ANY && type.getFlow() != packetFlow) {
            throw new DecoderException("Incorrect packet flow detected! " + packet);
        }
        if (debug) {
            LOGGER.info(MARKER, "IN: [{}:({})] {}", packet.getClass().getSimpleName(), type.getId(), packet);
        }
        return packet;
    }
}
