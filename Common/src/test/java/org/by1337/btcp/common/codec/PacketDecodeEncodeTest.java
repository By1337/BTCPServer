package org.by1337.btcp.common.codec;

import io.netty.buffer.Unpooled;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.impl.DisconnectPacket;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PacketDecodeEncodeTest {

    @Test
    void decodeEncodeTest() throws IOException {
        DisconnectPacket packet = new DisconnectPacket("test");

        ByteBuffer buffer = new ByteBuffer(Unpooled.buffer());

        buffer.writePacket(packet);

        DisconnectPacket read = (DisconnectPacket) buffer.readPacket();

        assertEquals(read, packet);
    }

}