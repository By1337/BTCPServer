package org.by1337.btcp.common.packet.impl.channel;

import org.by1337.btcp.common.annotations.OnlyInChannel;
import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.util.id.SpacedName;

import java.io.IOException;

@OnlyInChannel
@PacketInfo(packetFlow = PacketFlow.SERVER_BOUND)
public class CloseChannelPacket extends Packet {

    public CloseChannelPacket() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
    }
}

