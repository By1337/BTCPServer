package org.by1337.btcp.common.packet.impl;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.util.id.SpacedName;

import java.io.IOException;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class PacketForwardMessage extends Packet {
    private SpacedName to;
    private byte[] bytes;

    public PacketForwardMessage(SpacedName to, byte[] bytes) {
        this.to = to;
        this.bytes = bytes;
    }

    public PacketForwardMessage() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        to = byteBuf.readSpacedName();
        bytes = new byte[byteBuf.readVarInt()];
        byteBuf.readBytes(bytes);
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeSpacedName(to);
        byteBuf.writeVarInt(bytes.length);
        byteBuf.writeBytes(bytes);
    }

    public SpacedName to() {
        return to;
    }

    public byte[] bytes() {
        return bytes;
    }


    @Override
    public String toString() {
        return "PacketForwardMessage{" +
                "to=" + to +
                ", data=" + bytes.length + " bytes..." +
                '}';
    }
}
