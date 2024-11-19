package org.by1337.btcp.client.network.channel.forward;

import io.netty.buffer.ByteBuf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.by1337.blib.util.collection.IdentityHashSet;
import org.by1337.btcp.client.network.channel.AbstractClientChannel;
import org.by1337.btcp.client.network.channel.ClientChannelManager;
import org.by1337.btcp.client.util.CopyOnWriteIdentityHashSet;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.PacketForwardMessage;
import org.by1337.btcp.common.packet.impl.channel.ChannelStatusPacket;
import org.by1337.btcp.common.util.id.SpacedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ForwardChannelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("TCPClient");
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ForwardChannel forwardChannel;
    private final Map<SpacedName, SubChannel> channels = new ConcurrentHashMap<>();
    private EventListener eventListener;
    private final Plugin plugin;

    public ForwardChannelManager(ClientChannelManager clientChannelManager, Plugin plugin) {
        forwardChannel = new ForwardChannel(clientChannelManager);
        forwardChannel.register().sync(5_000);
        if (forwardChannel.getStatus() != ChannelStatusPacket.ChannelStatus.OPENED) {
            throw new IllegalStateException("Forward channel is not opened");
        }
        this.plugin = plugin;
    }

    public void onEnable() {
        if (eventListener != null) throw new IllegalStateException("EventListener already set");
        eventListener = new EventListener();
        plugin.getServer().getPluginManager().registerEvents(eventListener, plugin);
    }

    public void unregister() {
        if (eventListener != null) {
            HandlerList.unregisterAll(eventListener);
        }
        forwardChannel.unregister();
    }

    public SubChannel getOrCreateChannel(SpacedName id) {
        try {
            lock.writeLock().lock();
            return channels.computeIfAbsent(id, SubChannel::new);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private class EventListener implements Listener {

        @EventHandler
        public void onUnload(PluginDisableEvent event) {
            try {
                lock.readLock().lock();
                channels.values().forEach(c -> c.unregisterAll(event.getPlugin()));
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    public class SubChannel {
        private final Lock lock = new ReentrantLock();
        private final SpacedName id;
        private final Set<Listener> listeners = new CopyOnWriteIdentityHashSet<>(2);
        private final Map<Plugin, Set<Listener>> pluginToListeners = new IdentityHashMap<>(2);

        private boolean preferReadBytes;

        public SubChannel(SpacedName id) {
            this.id = id;
        }

        private void notifyListeners(PacketForwardMessage message) {
            for (Listener listener : listeners) {
                try {
                    if (listener instanceof ByteBufListener byteBufListener) {
                        byteBufListener.accept(message.buf());
                    } else if (listener instanceof BytesListener bytesListener) {
                        bytesListener.accept(message.bytes());
                    } else {
                        throw new IllegalArgumentException("Unknown listener type: " + listener.getClass().getName());
                    }
                } catch (Throwable t) {
                    Plugin plugin = getOwner(listener.getClass());
                    LOGGER.error("Error while notifying listener: {} class: {} owner: {} sub-channel: {}",
                            listener,
                            listener.getClass().getCanonicalName(),
                            plugin == null ? "unknown" : plugin.getName(),
                            id,
                            t
                    );
                }
            }
        }

        public void registerListener(@NotNull BytesListener listener) {
            registerListener((Listener) listener);
        }

        public void registerListener(@NotNull BytesListener listener, @Nullable Plugin owner) {
            registerListener((Listener) listener, owner);
        }

        public void registerListener(@NotNull ByteBufListener listener) {
            registerListener((Listener) listener);
        }

        public void registerListener(@NotNull ByteBufListener listener, @Nullable Plugin owner) {
            registerListener((Listener) listener, owner);
        }

        private void registerListener(@NotNull Listener listener) {
            registerListener(listener, getOwner(listener.getClass()));
        }

        private void registerListener(@NotNull Listener listener, @Nullable Plugin owner) {
            try {
                lock.lock();
                if (listeners.contains(listener)) {
                    throw new IllegalStateException("Listener already registered");
                }
                listeners.add(listener);
                this.updatePreferReadBytes();
                if (owner != null) {
                    pluginToListeners.computeIfAbsent(owner, k -> new IdentityHashSet<>()).add(listener);
                }
            } finally {
                lock.unlock();
            }
        }

        public void unregisterListener(@NotNull BytesListener listener) {
            unregisterListener((Listener) listener);
        }

        public void unregisterListener(@NotNull BytesListener listener, @Nullable Plugin owner) {
            unregisterListener((Listener) listener, owner);
        }

        public void unregisterListener(@NotNull ByteBufListener listener) {
            unregisterListener((Listener) listener);
        }

        public void unregisterListener(@NotNull ByteBufListener listener, @Nullable Plugin owner) {
            unregisterListener((Listener) listener, owner);
        }

        private void unregisterListener(@NotNull Listener listener) {
            unregisterListener(listener, getOwner(listener.getClass()));
        }

        private void unregisterListener(@NotNull Listener listener, @Nullable Plugin owner) {
            try {
                lock.lock();
                listeners.remove(listener);
                this.updatePreferReadBytes();
                if (owner != null) {
                    pluginToListeners.getOrDefault(owner, Collections.emptySet()).remove(listener);
                }
            } finally {
                lock.unlock();
            }
        }

        public void unregisterAll(Plugin owner) {
            try {
                lock.lock();
                var set = pluginToListeners.remove(owner);
                if (set != null) {
                    listeners.removeAll(set);
                    this.updatePreferReadBytes();
                }
            } finally {
                lock.unlock();
            }
        }

        private void updatePreferReadBytes() {
            for (Listener listener : listeners) {
                if (!(listener instanceof BytesListener)) {
                    preferReadBytes = false;
                    return;
                }
            }

            preferReadBytes = true;
        }

        public void send(byte[] data) {
            forwardChannel.send(new PacketForwardMessage(id, data, preferReadBytes));
        }

        public void send(ByteBuf data) {
            forwardChannel.send(new PacketForwardMessage(id, data, preferReadBytes));
        }

        private @Nullable Plugin getOwner(Class<?> clazz) {
            return clazz.getClassLoader() instanceof PluginClassLoader pcl ? pcl.getPlugin() : null;
        }

        private interface Listener {

        }

        @FunctionalInterface
        public interface BytesListener extends SubChannel.Listener {

            void accept(byte[] data);
        }

        @FunctionalInterface
        public interface ByteBufListener extends SubChannel.Listener {

            void accept(ByteBuf data);
        }
    }

    public class ForwardChannel extends AbstractClientChannel {
        public ForwardChannel(ClientChannelManager clientChannelManager) {
            super(clientChannelManager, new SpacedName("native", "forward"));
        }

        @Override
        public void onPacket(Packet packet) {
            if (packet instanceof PacketForwardMessage forwardMessage) {
                SubChannel subChannel;
                try {
                    lock.readLock().lock();
                    subChannel = channels.get(forwardMessage.to());
                } finally {
                    lock.readLock().unlock();
                }
                if (subChannel != null) {
                    subChannel.notifyListeners(forwardMessage);
                }
            }
        }

        @Override
        public @Nullable Packet onRequest(Packet packet) {
            return null;
        }
    }

}
