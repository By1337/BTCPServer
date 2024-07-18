package org.by1337.btcp.common.packet.impl;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;

import java.io.IOException;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class PacketPingResponse extends Packet {
    private int ping;

    public PacketPingResponse(int ping) {
        this.ping = ping;
    }

    public PacketPingResponse() {
    }

    public int getPing() {
        return ping;
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        ping = byteBuf.readVarInt();
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeVarInt(ping);
    }
}
