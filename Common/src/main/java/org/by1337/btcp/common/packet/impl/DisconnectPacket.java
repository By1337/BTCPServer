package org.by1337.btcp.common.packet.impl;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

@PacketInfo(packetFlow = PacketFlow.CLIENT_BOUND)
public class DisconnectPacket extends Packet {
    private String reason;

    public DisconnectPacket() {

    }

    public DisconnectPacket(@NotNull String reason) {
        this.reason = reason;
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        reason = byteBuf.readUtf();
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeUtf(reason);
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "DisconnectPacket{" +
               "reason='" + reason + '\'' +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisconnectPacket that = (DisconnectPacket) o;
        return Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reason);
    }
}
