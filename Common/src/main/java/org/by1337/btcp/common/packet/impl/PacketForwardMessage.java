package org.by1337.btcp.common.packet.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;
import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.util.id.SpacedName;

import java.io.IOException;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class PacketForwardMessage extends Packet implements ReferenceCounted {
    private SpacedName to;
    private byte[] bytes;
    private ByteBuf buf;
    private boolean preferReadBytes;

    public PacketForwardMessage(SpacedName to, byte[] bytes, boolean preferReadBytes) {
        this.to = to;
        this.bytes = bytes;
        this.preferReadBytes = preferReadBytes;
    }

    public PacketForwardMessage(SpacedName to, ByteBuf buf, boolean preferReadBytes) {
        this.to = to;
        this.buf = buf;
        this.preferReadBytes = preferReadBytes;
    }

    public PacketForwardMessage() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        to = byteBuf.readSpacedName();
        preferReadBytes = byteBuf.readBoolean();
        int length = byteBuf.readVarInt();
        if (preferReadBytes) {
            bytes = new byte[length];
            byteBuf.readBytes(bytes);
        } else {
            buf = byteBuf.readRetainedSlice(length);
        }
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeSpacedName(to);
        byteBuf.writeBoolean(preferReadBytes);
        if (bytes == null) {
            byteBuf.writeVarInt(buf.readableBytes());
            byteBuf.writeBytes(buf);
        } else {
            byteBuf.writeVarInt(bytes.length);
            byteBuf.writeBytes(bytes);
        }
    }

    @Override
    public int refCnt() {
        return buf == null ? -1 : buf.refCnt();
    }

    @Override
    public ReferenceCounted retain() {
        if (buf != null) {
            buf.retain();
        }

        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        if (buf != null) {
            buf.retain(increment);
        }

        return this;
    }

    @Override
    public ReferenceCounted touch() {
        if (buf != null) {
            buf.touch();
        }

        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        if (buf != null) {
            buf.touch(hint);
        }

        return this;
    }

    @Override
    public boolean release() {
        return buf != null && buf.release();
    }

    @Override
    public boolean release(int decrement) {
        return buf != null && buf.release(decrement);
    }

    public SpacedName to() {
        return to;
    }

    public byte[] bytes() {
        return bytes == null ? bytes = ByteBufUtil.getBytes(buf, 0, buf.readableBytes(), false) : bytes;
    }

    public ByteBuf buf() {
        return buf == null ? buf = Unpooled.wrappedBuffer(bytes) : buf;
    }

    @Override
    public String toString() {
        return "PacketForwardMessage{" +
                "to=" + to +
                ", data=" + (bytes == null ? buf : bytes.length + " bytes...") +
                '}';
    }
}
