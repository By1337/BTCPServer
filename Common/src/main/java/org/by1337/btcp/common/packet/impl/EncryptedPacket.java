package org.by1337.btcp.common.packet.impl;

import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.util.crypto.AESUtil;

import java.io.IOException;
import java.util.Objects;

@PacketInfo(packetFlow = PacketFlow.ANY)
public class EncryptedPacket extends Packet implements ReferenceCounted {
    private static String secretKey;
    private Packet packet;

    public EncryptedPacket(Packet packet) {
        this.packet = packet;
    }

    public EncryptedPacket() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        Objects.requireNonNull(secretKey, "secretKey is null!");
        byte[] encryptedData = new byte[byteBuf.readVarInt()];
        byteBuf.readBytes(encryptedData);

        byte[] packetData = AESUtil.decrypt(encryptedData, AESUtil.createKey(secretKey));

        ByteBuffer buffer = new ByteBuffer(Unpooled.wrappedBuffer(packetData));

        try {
            packet = buffer.readPacket();
        } finally {
            buffer.release();
        }
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        Objects.requireNonNull(secretKey, "secretKey is null!");
        ByteBuffer buffer = new ByteBuffer(Unpooled.buffer());
        try {
            buffer.writePacket(packet);
            byte[] packetData = new byte[buffer.readableBytes()];
            buffer.readBytes(packetData);

            byte[] encryptedData = AESUtil.encrypt(packetData, AESUtil.createKey(secretKey));


            byteBuf.writeVarInt(encryptedData.length);
            byteBuf.writeBytes(encryptedData);

        } finally {
            buffer.release();
        }
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

    public Packet getPacket() {
        return packet;
    }

    public static void setSecretKey(String secretKey) {
        EncryptedPacket.secretKey = secretKey;
    }

    @Override
    public String toString() {
        return "EncryptedPacket{" +
                "packet=" + packet +
                '}';
    }
}
