package org.by1337.btcp.server.network.channel;

import org.by1337.btcp.common.annotations.EventHandler;
import org.by1337.btcp.common.event.AbstractListener;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.RequestPacket;
import org.by1337.btcp.common.packet.impl.ResponsePacket;
import org.by1337.btcp.common.packet.impl.channel.ChannelStatusPacket;
import org.by1337.btcp.common.packet.impl.channel.ChanneledPacket;
import org.by1337.btcp.common.packet.impl.channel.OpenChannelPacket;
import org.by1337.btcp.common.util.id.SpacedName;
import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.by1337.btcp.server.dedicated.client.Client;
import org.by1337.btcp.server.event.ClientDisconnectEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ServerChannelManager extends AbstractListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerChannelManager.class);
    private final Map<SpacedName, AbstractServerChannel> channelMap = new HashMap<>();
    private final Map<SpacedName, Set<Client>> clientMap = new HashMap<>();
    private final Map<String, Set<SpacedName>> channelsByClient = new HashMap<>();
    private final DedicatedServer server;


    public ServerChannelManager(DedicatedServer server) {
        this.server = server;
        server.getEventManager().register(this);
    }

    public void registerChannel(SpacedName spacedName, AbstractServerChannel channel) {
        synchronized (this) {
            if (channelMap.containsKey(spacedName)) {
                throw new IllegalStateException(String.format("The channel with the name %s is already registered!", spacedName.asString()));
            }
            channelMap.put(spacedName, channel);
        }
    }

    public void unregisterChannel(SpacedName spacedName) {
        synchronized (this) {
            AbstractServerChannel channel = channelMap.remove(spacedName);
            if (channel == null) {
                throw new IllegalStateException(String.format("Channel named %s does not exist!", spacedName.asString()));
            }
            sendAll(spacedName, new ChannelStatusPacket(ChannelStatusPacket.ChannelStatus.CLOSED));
            clientMap.remove(spacedName);
        }
    }

    @NotNull
    public Set<Client> getClientsByChannel(SpacedName spacedName) {
        synchronized (this) {
            return Collections.unmodifiableSet(
                    Objects.requireNonNullElse(clientMap.get(spacedName), Collections.emptySet())
            );
        }
    }

    public void send(SpacedName spacedName, Client client, Packet packet) {
        client.send(new ChanneledPacket(spacedName, packet));
    }

    public void sendAll(SpacedName spacedName, Packet packet) {
        for (Client client : getClientsByChannel(spacedName)) {
            client.send(new ChanneledPacket(spacedName, packet));
        }
    }

    public void onPacket(Packet packet, Client client) {
        if (packet instanceof OpenChannelPacket openChannelPacket) {
            var channel = getChannel(openChannelPacket.getChannel());
            if (channel == null) {
                client.send(new ChanneledPacket(openChannelPacket.getChannel(), new ChannelStatusPacket(ChannelStatusPacket.ChannelStatus.UNKNOWN_CHANNEL)));
                return;
            }
            synchronized (this) {
                clientMap.computeIfAbsent(channel.getId(), k -> new HashSet<>()).add(client);
                channelsByClient.computeIfAbsent(client.getId(), k -> new HashSet<>()).add(channel.getId());
            }
            ChanneledClient channeledClient = new ChanneledClient(client, channel);
            channeledClient.send(new ChannelStatusPacket(ChannelStatusPacket.ChannelStatus.OPENED));
            channel.onConnect(channeledClient);
        } else if (packet instanceof ChanneledPacket channeledPacket) {
            Packet in = channeledPacket.getPacket();
            AbstractServerChannel channel = getChannel(channeledPacket.getChannel());
            ChanneledClient channeledClient = new ChanneledClient(client, channel);
            if (channel == null) {
                LOGGER.error("Client {} sent packet {} to a non-existent channel {}.", client.getId(), packet, channeledPacket.getChannel());
                client.disconnect(String.format("Channel %s does not exist!", channeledPacket.getChannel()));
                return;
            }
            if (in instanceof ResponsePacket responsePacket) {
                channel.onResponse(responsePacket, client);
            } else if (in instanceof RequestPacket requestPacket) {
                Packet request = requestPacket.getPacket();
                Packet response = channel.onRequest(request, channeledClient);
                channeledClient.send(new ResponsePacket(requestPacket.getUid(), response));
            } else {
                channel.onPacket(in, channeledClient);
            }
        } else {
            LOGGER.error("Client {} sent a packet {} that was not expected.", client.getId(), packet);
            client.disconnect("Not expected packet");
        }
    }

    private AbstractServerChannel getChannel(SpacedName spacedName) {
        synchronized (this) {
            return channelMap.get(spacedName);
        }
    }

    @EventHandler
    private void on(ClientDisconnectEvent event) {
        synchronized (this) {
            for (SpacedName spacedName : channelsByClient.getOrDefault(event.getClient().getId(), Collections.emptySet())) {
                var channel = channelMap.get(spacedName);
                if (channel != null) {
                    channel.onDisconnect0(event.getClient());
                }
            }
            channelsByClient.remove(event.getClient().getId());
        }
    }

}
