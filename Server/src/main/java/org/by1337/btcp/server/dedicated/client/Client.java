package org.by1337.btcp.server.dedicated.client;

import io.netty.channel.Channel;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.server.dedicated.DedicatedServer;

import java.net.SocketAddress;

public interface Client {
    void disconnect(String reason);
    void send(Packet packet);
    Channel getChannel();
    SocketAddress getAddress();
    String getId();
    DedicatedServer getServer();
    long getConnected();
    boolean isDisconnected();
}

