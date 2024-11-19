package org.by1337.btcp.common.packet.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
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
        int length = byteBuf.readVarInt();
        if (byteBuf.readBoolean()) {
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
    public void release() {
        if (buf != null) {
            buf.release();
        }
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
                (bytes == null ? buf : ", data=" + bytes.length + " bytes...") +
                '}';
    }
}
