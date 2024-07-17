package org.by1337.btcp.server.event;

import io.netty.channel.ChannelHandlerContext;
import org.by1337.btcp.common.event.Event;
import org.by1337.btcp.common.packet.Packet;

public class ClientLoginEvent implements Event {
    private final ChannelHandlerContext ctx;
    private final Packet firstPacket;
    private boolean canceled;
    private String reason;

    public ClientLoginEvent(ChannelHandlerContext ctx, Packet firstPacket) {
        this.ctx = ctx;
        this.firstPacket = firstPacket;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public Packet getFirstPacket() {
        return firstPacket;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
