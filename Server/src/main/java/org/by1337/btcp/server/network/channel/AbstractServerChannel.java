package org.by1337.btcp.server.network.channel;

import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.RequestPacket;
import org.by1337.btcp.common.packet.impl.ResponsePacket;
import org.by1337.btcp.common.packet.impl.channel.ChanneledPacket;
import org.by1337.btcp.common.util.id.SpacedName;
import org.by1337.btcp.server.dedicated.client.Client;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractServerChannel {
    private final AtomicInteger requestCounter = new AtomicInteger();
    private final ServerChannelManager serverChannelManager;
    private final SpacedName spacedName;
    private final Map<Integer, CompletableFuture<Optional<Packet>>> requestMap = new HashMap<>();
    private final Map<String, Set<Integer>> requestMapByClient = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AbstractServerChannel(ServerChannelManager serverChannelManager, SpacedName spacedName) {
        this.serverChannelManager = serverChannelManager;
        this.spacedName = spacedName;
    }

    public void register() {
        serverChannelManager.registerChannel(spacedName, this);
    }

    public void unregister() {
        serverChannelManager.unregisterChannel(spacedName);
    }

    public abstract void onPacket(Packet packet, ChanneledClient client);

    public CompletableFuture<Optional<Packet>> request(ChanneledClient client, Packet packet) {
        return request(client.getHandle(), packet);
    }

    public CompletableFuture<Optional<Packet>> request(Client client, Packet packet) {
        return this.request(client, packet, 30, TimeUnit.SECONDS);
    }

    public CompletableFuture<Optional<Packet>> request(Client client, Packet packet, long timeout, TimeUnit timeUnit) {
        CompletableFuture<Optional<Packet>> future = new CompletableFuture<>();
        int uid;
        synchronized (this) {
            uid = requestCounter.getAndIncrement();
            requestMap.put(uid, future);
            requestMapByClient.computeIfAbsent(client.getId(), k -> new HashSet<>()).add(uid);
        }
        client.send(new ChanneledPacket(spacedName, new RequestPacket(uid, packet)));
        scheduler.schedule(() -> response(uid, client.getId(), null), timeout, timeUnit);
        return future;
    }

    public abstract Packet onRequest(Packet packet, ChanneledClient client);

    public void onResponse(ResponsePacket responsePacket, Client client) {
        response(responsePacket.getUid(), client.getId(), responsePacket.getPacket());
    }

    private void response(int uid, String clientId, @Nullable Packet response) {
        CompletableFuture<Optional<Packet>> future;
        synchronized (this) {
            future = requestMap.remove(uid);
            Set<Integer> clientRequests = requestMapByClient.get(clientId);
            if (clientRequests != null) {
                clientRequests.remove(uid);
                if (clientRequests.isEmpty()) {
                    requestMapByClient.remove(clientId);
                }
            }
        }
        if (future != null) {
            future.complete(Optional.ofNullable(response));
        }
    }

    public final void onDisconnect0(Client client) {
        cleanupRequestsForClient(client.getId());
        onDisconnect(new ChanneledClient(client, this));
    }

    private void cleanupRequestsForClient(String clientId) {
        synchronized (this) {
            Set<Integer> clientRequests = requestMapByClient.remove(clientId);
            if (clientRequests != null) {
                for (Integer uid : clientRequests) {
                    CompletableFuture<Optional<Packet>> future = requestMap.remove(uid);
                    if (future != null) {
                        future.complete(Optional.empty());
                    }
                }
            }
        }
    }

    public void onDisconnect(ChanneledClient client) {
    }

    public void onConnect(ChanneledClient client) {
    }

    public List<ChanneledClient> getClients() {
        return serverChannelManager.getClientsByChannel(spacedName).stream().map(c -> new ChanneledClient(c, this)).toList();
    }

    public SpacedName getId() {
        return spacedName;
    }
}
