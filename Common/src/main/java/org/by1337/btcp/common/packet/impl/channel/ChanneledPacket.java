package org.by1337.btcp.common.packet.impl.channel;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.util.id.SpacedName;

import java.io.IOException;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class ChanneledPacket extends Packet {
    private SpacedName channel;
    private Packet packet;

    public ChanneledPacket(SpacedName channel, Packet packet) {
        this.channel = channel;
        this.packet = packet;
    }

    public ChanneledPacket() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        channel = byteBuf.readSpacedName();
        packet = byteBuf.readPacket();
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeSpacedName(channel);
        byteBuf.writePacket(packet);
    }

    public SpacedName getChannel() {
        return channel;
    }

    public Packet getPacket() {
        return packet;
    }

    @Override
    public String toString() {
        return "ChanneledPacket{" +
               "channel=" + channel +
               ", packet=" + packet +
               '}';
    }
}
