package org.by1337.btcp.server.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.TimeoutException;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.DisconnectPacket;
import org.by1337.btcp.common.packet.impl.EncryptedPacket;
import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.by1337.btcp.server.dedicated.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.logging.Level;

public class Connection extends SimpleChannelInboundHandler<Packet> implements Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);

    private final Channel channel;
    private final SocketAddress address;
    private final String id;
    private final DedicatedServer server;
    private final long connected;
    private boolean disconnected;

    public Connection(Channel channel, SocketAddress address, String id, DedicatedServer server) {
        if (!channel.isOpen()) {
            throw new IllegalArgumentException("channel is closed!");
        }
        this.channel = channel;
        this.address = address;
        this.id = id;
        this.server = server;
        connected = System.currentTimeMillis();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet packet) throws Exception {
        server.onPacket(packet, this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        this.disconnect("End of stream");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        disconnect("connection unregister");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof TimeoutException) {
            this.disconnect("Timed out");
        } else {
            this.disconnect("Internal Exception: " + cause);
            LOGGER.error("An error occurred in the {} connection", this, cause);
        }
    }

    @Override
    public void disconnect(String reason) {
        if (!disconnected) {
            try {
                if (channel.isOpen()) {
                    send(new DisconnectPacket(reason));
                }
                this.channel.close().awaitUninterruptibly();
            } finally {
                disconnected = true;
                server.getClientList().disconnect(this, reason);
            }
        }
    }

    @Override
    public void send(Packet packet) {
        if (channel.isOpen()) {
            channel.writeAndFlush(packet);
        }
    }
    @Override
    public void sendEncrypted(Packet packet) {
        send(new EncryptedPacket(packet));
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public SocketAddress getAddress() {
        return address;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public DedicatedServer getServer() {
        return server;
    }

    @Override
    public long getConnected() {
        return connected;
    }

    @Override
    public boolean isDisconnected() {
        return disconnected;
    }

    @Override
    public String toString() {
        return "Connection{" +
               "channel=" + channel +
               ", address=" + address +
               ", id='" + id + '\'' +
               ", connected=" + connected +
               ", disconnected=" + disconnected +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return connected == that.connected && disconnected == that.disconnected && Objects.equals(channel, that.channel) && Objects.equals(address, that.address) && Objects.equals(id, that.id) && Objects.equals(server, that.server);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, address, id, server, connected, disconnected);
    }
}
