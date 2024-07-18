package org.by1337.btcp.common.packet.impl;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;

import java.io.IOException;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class PacketPingRequest extends Packet {
    private long time;

    public PacketPingRequest(long time) {
        this.time = time;
    }

    public PacketPingRequest() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        time = byteBuf.readVarLong();
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeVarLong(time);
    }

    public long getTime() {
        return time;
    }
}
