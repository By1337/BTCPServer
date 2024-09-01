package org.by1337.btcp.common.packet;

import org.by1337.btcp.common.annotations.PacketInfo;
import org.by1337.btcp.common.packet.impl.*;
import org.by1337.btcp.common.packet.impl.channel.*;
import org.by1337.btcp.common.util.id.SpacedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PacketRegistry<T extends Packet> {
    private static final PacketRegistry<? extends Packet> instance = new PacketRegistry<>();
    private final Map<Integer, PacketType<T>> lookupByIntId = new HashMap<>();
    private final Map<Class<? extends Packet>, PacketType<T>> lookupByClass = new HashMap<>();

    private PacketRegistry() {
    }

    public PacketType<T> register(Class<? extends Packet> clazz, PacketType<T> type) {
        if (lookupByClass.containsKey(clazz)) {
            throw new IllegalArgumentException("Packet " + clazz.getCanonicalName() + " is already registered");
        }
        PacketType<T> actual;
        if ((actual = lookupByIntId.putIfAbsent(type.getIdAsInt(), type)) != null) {
            throw new IllegalArgumentException(
                    String.format(
                            "Duplicate int id! PacketType %s and %s are conflicting!",
                            type, actual
                    )
            );
        }
        lookupByClass.putIfAbsent(clazz, type);
        return type;
    }

    @Nullable
    public PacketType<T> lookup(int id) {
        return lookupByIntId.get(id);
    }

    @Nullable
    public PacketType<T> lookup(Class<? extends Packet> clazz) {
        return lookupByClass.get(clazz);
    }

    @Nullable
    public PacketType<T> lookup(SpacedName spacedName) {
        return lookup(spacedName.hashCode());
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public PacketType<T>[] values() {
        return lookupByIntId.values().toArray(new PacketType[0]);
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Packet>[] packets(){
        return lookupByClass.keySet().toArray(new Class[0]);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> PacketRegistry<T> get() {
        return (PacketRegistry<T>) instance;
    }

    static {
        PacketTypeRegister.get()
                .flow(PacketFlow.CLIENT_BOUND)
                .add(DisconnectPacket.class, DisconnectPacket::new, "native:disconnect")
                .add(PacketAuthResponse.class, PacketAuthResponse::new, "native:auth_response")
                .add(ChannelStatusPacket.class, ChannelStatusPacket::new, "native:channel_status")
                .flow(PacketFlow.SERVER_BOUND)
                .add(PacketAuth.class, PacketAuth::new, "native:auth")
                .add(OpenChannelPacket.class, OpenChannelPacket::new, "native:open_channel")
                .add(CloseChannelPacket.class, CloseChannelPacket::new, "native:close_channel")
                .flow(PacketFlow.ANY)
                .add(ChanneledPacket.class, ChanneledPacket::new, "native:channeled_packet")
                .add(PacketPingRequest.class, PacketPingRequest::new, "native:ping_request")
                .add(PacketPingResponse.class, PacketPingResponse::new, "native:ping_response")
                .add(RequestPacket.class, RequestPacket::new, "native:request")
                .add(ResponsePacket.class, ResponsePacket::new, "native:response")
                .add(EncryptedPacket.class, EncryptedPacket::new, "native:encrypted")
        ;
    }

    public static class PacketTypeRegister<T extends Packet> {
        private final PacketRegistry<T> registry;

        private PacketTypeRegister(PacketRegistry<T> registry) {
            this.registry = registry;
        }

        public PacketTypeRegisterWithFlow flow(PacketFlow flow) {
            return new PacketTypeRegisterWithFlow(flow);
        }

        public static <T extends Packet> PacketTypeRegister<T> get() {
            return new PacketTypeRegister<>(PacketRegistry.get());
        }

        public class PacketTypeRegisterWithFlow {
            private final PacketFlow flow;

            private PacketTypeRegisterWithFlow(PacketFlow flow) {
                this.flow = flow;
            }

            public PacketTypeRegisterWithFlow add(@NotNull Class<? extends T> clazz, @NotNull Supplier<T> creator, @NotNull String name) {
                return add(clazz, creator, SpacedName.parse(name));
            }

            public PacketTypeRegisterWithFlow add(@NotNull Class<? extends T> clazz, @NotNull Supplier<T> creator, @NotNull String space, @NotNull String name) {
                return add(clazz, creator, new SpacedName(space, name));
            }

            public PacketTypeRegisterWithFlow add(@NotNull Class<? extends T> clazz, @NotNull Supplier<T> creator, @NotNull SpacedName id) {
                registry.register(clazz, new PacketType<>(creator, id, flow));
                return this;
            }

            public PacketTypeRegisterWithFlow flow(PacketFlow flow) {
                return new PacketTypeRegisterWithFlow(flow);
            }
        }
    }

}
