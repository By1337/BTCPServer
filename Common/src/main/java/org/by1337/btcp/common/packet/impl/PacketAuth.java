package org.by1337.btcp.common.packet.impl;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.util.crypto.AESUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@PacketInfo(packetFlow = PacketFlow.SERVER_BOUND)
public class PacketAuth extends Packet {
    @Nullable
    private String id;
    private String password;
    private transient String secretKey;

    public PacketAuth(@Nullable String id, String password, String secretKey) {
        this.id = id;
        this.password = password;
        this.secretKey = secretKey;
    }

    public PacketAuth() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        id = byteBuf.readOptional(ByteBuffer::readUtf).orElse(null);
        password = byteBuf.readUtf();
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeOptional(id, ByteBuffer::writeUtf);
        byteBuf.writeUtf(AESUtil.encrypt(password, secretKey));
    }


    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public String getPassword() {
        return password;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketAuth that = (PacketAuth) o;
        return Objects.equals(id, that.id) && Objects.equals(password, that.password) && Objects.equals(secretKey, that.secretKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, password, secretKey);
    }

    @Override
    public String toString() {
        return "PacketAuth{" +
               "id='" + id + '\'' +
               ", password='***'" +
               ", secretKey='***'" +
               '}';
    }
}
