package org.by1337.btcp.server.network.channel.impl;

import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.PacketPingRequest;
import org.by1337.btcp.common.packet.impl.PacketPingResponse;
import org.by1337.btcp.common.util.id.SpacedName;
import org.by1337.btcp.server.network.channel.AbstractServerChannel;
import org.by1337.btcp.server.network.channel.ChanneledClient;
import org.by1337.btcp.server.network.channel.ServerChannelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MainServerChannel extends AbstractServerChannel {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainServerChannel.class);

    public MainServerChannel(ServerChannelManager serverChannelManager, SpacedName spacedName) {
        super(serverChannelManager, spacedName);
    }

    @Override
    public void onPacket(Packet packet, ChanneledClient client) {

    }

    @Override
    public Packet onRequest(Packet packet, ChanneledClient client) {
        if (packet instanceof PacketPingRequest pingRequest) {
            return new PacketPingResponse((int) (System.currentTimeMillis() - pingRequest.getTime()));
        }
        return null;
    }

    @Override
    public void onConnect(ChanneledClient client) {
        client.request(new PacketPingRequest(System.currentTimeMillis())).thenAccept(packetOpt -> {
            if (packetOpt.isPresent()) {
                if (packetOpt.get() instanceof PacketPingResponse packetPingResponse) {
                    LOGGER.info("Ping to {} {} ms.", client.getId(), packetPingResponse.getPing());
                }
            }
        });
    }
}
