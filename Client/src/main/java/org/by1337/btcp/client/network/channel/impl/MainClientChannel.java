package org.by1337.btcp.client.network.channel.impl;

import org.by1337.btcp.client.network.channel.AbstractClientChannel;
import org.by1337.btcp.client.network.channel.ClientChannelManager;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.PacketPingRequest;
import org.by1337.btcp.common.packet.impl.PacketPingResponse;
import org.by1337.btcp.common.util.id.SpacedName;
import org.jetbrains.annotations.Nullable;

public final class MainClientChannel extends AbstractClientChannel {
    public MainClientChannel(ClientChannelManager clientChannelManager, SpacedName spacedName) {
        super(clientChannelManager, spacedName);
    }

    @Override
    public void onPacket(Packet packet) {

    }

    @Override
    public @Nullable Packet onRequest(Packet packet) {
        if (packet instanceof PacketPingRequest pingRequest) {
            return new PacketPingResponse((int) (System.currentTimeMillis() - pingRequest.getTime()));
        }
        return null;
    }
}
