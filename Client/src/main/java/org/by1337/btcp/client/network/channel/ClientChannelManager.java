package org.by1337.btcp.client.network.channel;

import org.by1337.btcp.client.network.Connection;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.RequestPacket;
import org.by1337.btcp.common.packet.impl.ResponsePacket;
import org.by1337.btcp.common.packet.impl.channel.ChannelStatusPacket;
import org.by1337.btcp.common.packet.impl.channel.ChanneledPacket;
import org.by1337.btcp.common.packet.impl.channel.CloseChannelPacket;
import org.by1337.btcp.common.packet.impl.channel.OpenChannelPacket;
import org.by1337.btcp.common.util.id.SpacedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ClientChannelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("BTCPServer");
    private final Connection connection;

    private final Map<SpacedName, AbstractClientChannel> channelMap = new HashMap<>();

    public ClientChannelManager(Connection connection) {
        this.connection = connection;
    }

    public void registerChannel(SpacedName spacedName, AbstractClientChannel channel) {
        synchronized (this) {
            if (channelMap.containsKey(spacedName)) {
                throw new IllegalStateException(String.format("The channel with the name %s is already registered!", spacedName.asString()));
            }
            channelMap.put(spacedName, channel);
        }
        connection.send(new OpenChannelPacket(spacedName));
    }

    public void unregisterChannel(SpacedName spacedName) {
        synchronized (this) {
            AbstractClientChannel channel = channelMap.remove(spacedName);
            if (channel == null) {
                throw new IllegalStateException(String.format("Channel named %s does not exist!", spacedName.asString()));
            }
            if (channel.getStatus() == ChannelStatusPacket.ChannelStatus.OPENED) {
                channel.send(new CloseChannelPacket());
            }
            channel.setStatus(ChannelStatusPacket.ChannelStatus.CLOSED);
        }
    }

    public void onPacket(Packet packet) {
        if (packet instanceof ChanneledPacket channeledPacket) {
            var channel = getChannel(channeledPacket.getChannel());
            if (channel == null) return;
            Packet in = channeledPacket.getPacket();
            if (in instanceof ChannelStatusPacket statusPacket) {
                channel.setStatus(statusPacket.getStatus());
            } else if (in instanceof ResponsePacket responsePacket) {
                channel.response(responsePacket.getUid(), responsePacket.getPacket());
            } else if (in instanceof RequestPacket requestPacket) {
                channel.onRequestAsync(requestPacket.getPacket()).whenComplete((p, t) -> {
                    if (t != null) {
                        LOGGER.error("Failed to request channel {}", channel.getId(), t);
                    }
                    channel.send(new ResponsePacket(requestPacket.getUid(), p));
                });

            } else {
                channel.onPacket(in);
            }
        }
    }

    private AbstractClientChannel getChannel(SpacedName spacedName) {
        synchronized (this) {
            return channelMap.get(spacedName);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
