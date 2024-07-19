package org.by1337.btcp.common.packet.impl.channel;

import org.by1337.btcp.common.annotations.OnlyInChannel;
import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;

import java.io.IOException;
@OnlyInChannel
@PacketInfo(packetFlow = PacketFlow.CLIENT_BOUND)
public class ChannelStatusPacket extends Packet {
    private ChannelStatus status;

    public ChannelStatusPacket(ChannelStatus status) {
        this.status = status;
    }

    public ChannelStatusPacket() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        status = byteBuf.readEnum(ChannelStatus.class);
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeEnum(status);
    }

    public ChannelStatus getStatus() {
        return status;
    }

    public enum ChannelStatus {
        OPENED,
        CLOSED,
        UNKNOWN_CHANNEL
    }

}
