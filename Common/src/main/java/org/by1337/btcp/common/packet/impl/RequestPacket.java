package org.by1337.btcp.common.packet.impl;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;

import java.io.IOException;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class RequestPacket extends Packet {
    private int uid;
    private Packet packet;

    public RequestPacket(int uid, Packet packet) {
        this.uid = uid;
        this.packet = packet;
    }

    public RequestPacket() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        uid = byteBuf.readVarInt();
        packet = byteBuf.readPacket();
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeVarInt(uid);
        byteBuf.writePacket(packet);
    }

    public int getUid() {
        return uid;
    }

    public Packet getPacket() {
        return packet;
    }

    @Override
    public String toString() {
        return "RequestPacket{" +
                "uid=" + uid +
                ", packet=" + packet +
                '}';
    }
}
