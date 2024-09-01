package org.by1337.btcp.client.network.channel;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.EncryptedPacket;
import org.by1337.btcp.common.packet.impl.RequestPacket;
import org.by1337.btcp.common.packet.impl.channel.ChannelStatusPacket;
import org.by1337.btcp.common.packet.impl.channel.ChanneledPacket;
import org.by1337.btcp.common.util.id.SpacedName;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractClientChannel {
    private final AtomicInteger requestCounter = new AtomicInteger();
    private final ClientChannelManager clientChannelManager;
    private final SpacedName spacedName;
    private final Map<Integer, CompletableFuture<Optional<Packet>>> requestMap = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ChannelStatusPacket.ChannelStatus status;
    private final Lock statusLock = new Lock();

    public AbstractClientChannel(ClientChannelManager clientChannelManager, SpacedName spacedName) {
        this.clientChannelManager = clientChannelManager;
        this.spacedName = spacedName;
    }

    @CanIgnoreReturnValue
    public Lock register() {
        clientChannelManager.registerChannel(spacedName, this);
        return statusLock;
    }

    public void unregister() {

    }

    void setStatus(ChannelStatusPacket.ChannelStatus status) {
        this.status = status;
        statusLock.update();
    }

    public abstract void onPacket(Packet packet);

    public CompletableFuture<Optional<Packet>> request(Packet packet, long timeout, TimeUnit timeUnit) {
        CompletableFuture<Optional<Packet>> future = new CompletableFuture<>();
        int uid;
        synchronized (this) {
            uid = requestCounter.getAndIncrement();
            requestMap.put(uid, future);
        }
        send(new RequestPacket(uid, packet));
        scheduler.schedule(() -> response(uid, null), timeout, timeUnit);
        return future;
    }

    public void response(int uid, @Nullable Packet response) {
        CompletableFuture<Optional<Packet>> future;
        synchronized (this) {
            future = requestMap.remove(uid);
        }
        if (future != null) {
            future.complete(Optional.ofNullable(response));
        }
    }

    public abstract @Nullable Packet onRequest(Packet packet);

    public void send(Packet packet) {
        clientChannelManager.getConnection().send(new ChanneledPacket(spacedName, packet));
    }

    public void sendEncrypted(Packet packet) {
        send(new EncryptedPacket(packet));
    }

    public SpacedName getId() {
        return spacedName;
    }

    public ChannelStatusPacket.ChannelStatus getStatus() {
        return status;
    }

    public static class Lock {
        private final Object updateStatusLock = new Object();

        public void sync() {
            sync(0L);
        }

        public void sync(long timeoutMillis) {
            try {
                synchronized (updateStatusLock) {
                    updateStatusLock.wait(timeoutMillis);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        void update() {
            synchronized (updateStatusLock) {
                updateStatusLock.notifyAll();
            }
        }
    }
}
