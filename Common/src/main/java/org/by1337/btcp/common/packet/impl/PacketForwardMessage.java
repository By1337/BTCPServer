package org.by1337.btcp.common.packet.impl;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.util.id.SpacedName;

import java.io.IOException;
import java.util.Arrays;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class PacketForwardMessage extends Packet {
    private SpacedName to;
    private byte[] data;

    public PacketForwardMessage(SpacedName to, byte[] data) {
        this.to = to;
        this.data = data;
    }

    public PacketForwardMessage() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        to = byteBuf.readSpacedName();
        data = new byte[byteBuf.readVarInt()];
        byteBuf.readBytes(data);
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeSpacedName(to);
        byteBuf.writeVarInt(data.length);
        byteBuf.writeBytes(data);
    }

    public SpacedName to() {
        return to;
    }

    public byte[] data() {
        return data;
    }

    @Override
    public String toString() {
        return "PacketForwardMessage{" +
                "to=" + to +
                ", data=" + data.length + " bytes..." +
                '}';
    }
}
