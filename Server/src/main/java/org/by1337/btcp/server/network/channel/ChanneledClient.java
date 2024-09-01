package org.by1337.btcp.server.network.channel;

import io.netty.channel.Channel;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.EncryptedPacket;
import org.by1337.btcp.common.packet.impl.channel.ChanneledPacket;
import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.by1337.btcp.server.dedicated.client.Client;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ChanneledClient implements Client {
    private final Client wrapped;
    private final AbstractServerChannel channel;

    public ChanneledClient(Client wrapped, AbstractServerChannel channel) {
        this.wrapped = wrapped;
        this.channel = channel;
    }

    public CompletableFuture<Optional<Packet>> request(Packet packet, long timeout, TimeUnit timeUnit){
        return channel.request(wrapped, packet, timeout, timeUnit);
    }
    public CompletableFuture<Optional<Packet>> request(Packet packet){
        return channel.request(wrapped, packet);
    }


    @Override
    public void disconnect(String reason) {
        wrapped.disconnect(reason);
    }

    @Override
    public void send(Packet packet) {
        wrapped.send(new ChanneledPacket(channel.getId(), packet));
    }
    @Override
    public void sendEncrypted(Packet packet) {
        send(new EncryptedPacket(packet));
    }

    @Override
    public Channel getChannel() {
        return wrapped.getChannel();
    }

    @Override
    public SocketAddress getAddress() {
        return wrapped.getAddress();
    }

    @Override
    public String getId() {
        return wrapped.getId();
    }

    @Override
    public DedicatedServer getServer() {
        return wrapped.getServer();
    }

    @Override
    public long getConnected() {
        return wrapped.getConnected();
    }

    @Override
    public boolean isDisconnected() {
        return wrapped.isDisconnected();
    }

    public Client getHandle() {
        return wrapped;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChanneledClient that = (ChanneledClient) o;
        return Objects.equals(wrapped, that.wrapped) && Objects.equals(channel.getId(), that.channel.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrapped, channel.getId());
    }
}
