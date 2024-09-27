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
    private String id;
    private int threshold;
    private int compressionLvl;

    public PacketAuthResponse(Response response, String id, int threshold, int compressionLvl) {
        this.response = response;
        this.id = id;
        this.threshold = threshold;
        this.compressionLvl = compressionLvl;
    }

    public PacketAuthResponse() {
    }

    @Override
    public void read(ByteBuffer byteBuf) throws IOException {
        response = byteBuf.readEnum(Response.class);
        id = byteBuf.readUtf();
        threshold = byteBuf.readVarInt();
        compressionLvl = byteBuf.readVarInt();
    }

    @Override
    public void write(ByteBuffer byteBuf) throws IOException {
        byteBuf.writeEnum(response);
        byteBuf.writeUtf(id);
        byteBuf.writeVarInt(threshold);
        byteBuf.writeVarInt(compressionLvl);
    }

    public enum Response {
        SUCCESSFULLY,
        FAILED
    }

    public Response getResponse() {
        return response;
    }

    public String getId() {
        return id;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getCompressionLvl() {
        return compressionLvl;
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
