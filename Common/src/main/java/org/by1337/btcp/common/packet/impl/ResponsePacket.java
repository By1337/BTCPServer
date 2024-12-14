package org.by1337.btcp.common.packet.impl;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class ResponsePacket extends Packet {
    private int uid;
    private @Nullable Packet packet;

    public ResponsePacket(int uid, @Nullable Packet packet) {
        this.uid = uid;
        this.packet = packet;
    }

    public ResponsePacket() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        uid = byteBuf.readVarInt();
        packet = byteBuf.readOptional(ByteBuffer::readPacket).orElse(null);
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeVarInt(uid);
        byteBuf.writeOptional(packet, ByteBuffer::writePacket);
    }

    public int getUid() {
        return uid;
    }

    public @Nullable Packet getPacket() {
        return packet;
    }

    @Override
    public String toString() {
        return "ResponsePacket{" +
                "uid=" + uid +
                ", packet=" + packet +
                '}';
    }
}
