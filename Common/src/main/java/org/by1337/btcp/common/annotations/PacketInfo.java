package org.by1337.btcp.common.annotations;

import org.by1337.btcp.common.packet.PacketFlow;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface PacketInfo {
    PacketFlow packetFlow();
}
