package org.by1337.btcp.common.packet.impl;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class ResponsePacket extends Packet implements ReferenceCounted {
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

    @Override
    public int refCnt() {
        return ReferenceCountUtil.refCnt(packet);
    }

    @Override
    public ReferenceCounted retain() {
        ReferenceCountUtil.retain(packet);
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        ReferenceCountUtil.retain(packet, increment);
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        ReferenceCountUtil.touch(packet);
        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        ReferenceCountUtil.touch(packet, hint);
        return this;
    }

    @Override
    public boolean release() {
        return ReferenceCountUtil.release(packet);
    }

    @Override
    public boolean release(int decrement) {
        return ReferenceCountUtil.release(packet, decrement);
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
