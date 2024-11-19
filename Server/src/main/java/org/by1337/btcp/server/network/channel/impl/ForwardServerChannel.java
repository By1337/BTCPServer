package org.by1337.btcp.server.network.channel.impl;

import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.PacketForwardMessage;
import org.by1337.btcp.common.util.id.SpacedName;
import org.by1337.btcp.server.network.channel.AbstractServerChannel;
import org.by1337.btcp.server.network.channel.ChanneledClient;
import org.by1337.btcp.server.network.channel.ServerChannelManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardServerChannel extends AbstractServerChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForwardServerChannel.class);

    public ForwardServerChannel(ServerChannelManager serverChannelManager, SpacedName spacedName) {
        super(serverChannelManager, spacedName);
    }


    @Override
    public void onPacket(Packet packet, ChanneledClient client) {
        if (packet instanceof PacketForwardMessage forwardMessage){
            getClients().forEach(c -> {
                if (c.getChannel() != client.getChannel()){
                    c.send(forwardMessage);
                }
            });
        }else {
            LOGGER.warn("Client {} sent an unexpected packet to {}", client.getId(), packet);
        }
    }

    @Override
    public @Nullable Packet onRequest(Packet packet, ChanneledClient client) {
        return null;
    }
}
