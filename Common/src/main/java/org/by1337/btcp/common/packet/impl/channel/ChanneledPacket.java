package org.by1337.btcp.common.packet.impl.channel;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.util.id.SpacedName;

import java.io.IOException;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class ChanneledPacket extends Packet implements ReferenceCounted {
    private SpacedName channel;
    private Packet packet;

    public ChanneledPacket(SpacedName channel, Packet packet) {
        this.channel = channel;
        this.packet = packet;
    }

    public ChanneledPacket() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        channel = byteBuf.readSpacedName();
        packet = byteBuf.readPacket();
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeSpacedName(channel);
        byteBuf.writePacket(packet);
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

    public SpacedName getChannel() {
        return channel;
    }

    public Packet getPacket() {
        return packet;
    }

    @Override
    public String toString() {
        return "ChanneledPacket{" +
               "channel=" + channel +
               ", packet=" + packet +
               '}';
    }
}
