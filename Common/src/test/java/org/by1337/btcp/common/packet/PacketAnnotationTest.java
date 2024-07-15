package org.by1337.btcp.common.packet;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.junit.jupiter.api.Test;

class PacketAnnotationTest {
    @Test
    void packetAnnotationTest() {
        for (Class<? extends Packet> packet : PacketRegistry.get().packets()) {
            if (!packet.isAnnotationPresent(PacketInfo.class)) {
                throw new IllegalArgumentException(String.format("Class %s is not annotated with PacketInfo", packet.getCanonicalName()));
            }
            PacketInfo info = packet.getAnnotation(PacketInfo.class);
            if (info.packetFlow() != PacketRegistry.get().lookup(packet).getFlow()) {
                throw new IllegalArgumentException(String.format("Class %s has incorrect packet flow in the annotation!", packet.getCanonicalName()));
            }
        }
    }
}