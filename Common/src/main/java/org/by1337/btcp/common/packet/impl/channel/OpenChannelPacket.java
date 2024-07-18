package org.by1337.btcp.common.packet.impl.channel;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.util.id.SpacedName;

import java.io.IOException;
@PacketInfo(packetFlow = PacketFlow.SERVER_BOUND)
public class OpenChannelPacket extends Packet {
    private SpacedName channel;

    public OpenChannelPacket(SpacedName channel) {
        this.channel = channel;
    }

    public OpenChannelPacket() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        channel = byteBuf.readSpacedName();
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeSpacedName(channel);
    }

    public SpacedName getChannel() {
        return channel;
    }
}
