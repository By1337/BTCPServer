package org.by1337.btcp.common.packet.impl;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;

import java.io.IOException;
import java.util.Objects;
@PacketInfo(packetFlow = PacketFlow.CLIENT_BOUND)
public class PacketAuthResponse extends Packet {
    private Response response;

    public PacketAuthResponse(Response response) {
        this.response = response;
    }

    public PacketAuthResponse() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        response = byteBuf.readEnum(Response.class);
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeEnum(response);
    }

    public enum Response {
        SUCCESSFULLY,
        FAILED
    }

    public Response getResponse() {
        return response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketAuthResponse that = (PacketAuthResponse) o;
        return response == that.response;
    }

    @Override
    public int hashCode() {
        return Objects.hash(response);
    }

    @Override
    public String toString() {
        return "PacketAuthResponse{" +
               "response=" + response +
               '}';
    }
}
