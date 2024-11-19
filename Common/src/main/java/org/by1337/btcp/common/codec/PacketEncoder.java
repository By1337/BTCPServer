package org.by1337.btcp.common.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
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

public class PacketEncoder extends MessageToByteEncoder<Packet> {
    private static final Marker MARKER = MarkerFactory.getMarker("[PACKET_SENT]");
    private final static Logger LOGGER = LoggerFactory.getLogger(PacketEncoder.class);

    private final boolean debug;
    private final PacketFlow packetFlow;

    public PacketEncoder(boolean debug, PacketFlow packetFlow) {
        this.debug = debug;
        this.packetFlow = packetFlow;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf) throws Exception {
        try {
            PacketType<?> type = PacketRegistry.get().lookup(packet.getClass());
            if (type == null) {
                throw new IOException("Can't serialize unregistered packet");
            }
            if (type.getFlow() != PacketFlow.ANY && type.getFlow() != packetFlow) {
                throw new EncoderException("Incorrect packet flow detected! " + packet);
            }
            ByteBuffer buf = new ByteBuffer(byteBuf);
            buf.writePacket(packet);
            if (debug) {
                LOGGER.info(MARKER, "OUT: [{}:({})] {}", packet.getClass().getSimpleName(), type.getId(), packet);
            }
        } catch (Throwable e) {
            LOGGER.error(MARKER, "Failed to encode packet {}", packet.getClass().getSimpleName(), e);
            throw e;
        } finally {
            packet.release();
        }
    }
}
