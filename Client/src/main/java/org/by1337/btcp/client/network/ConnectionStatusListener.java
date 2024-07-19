package org.by1337.btcp.client.network;

import org.by1337.btcp.common.packet.Packet;

public interface ConnectionStatusListener {
    void onPacket(Packet packet);
    void onDisconnect();
}
