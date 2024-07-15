package org.by1337.btcp.common.packet;

import org.by1337.btcp.common.packet.impl.DisconnectPacket;
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
    public static <T extends Packet> PacketRegistry<T> get() {
        return (PacketRegistry<T>) instance;
    }

    static {
        PacketRegisterBuilder.get()
                .of(PacketFlow.CLIENT_BOUND)
                .register(DisconnectPacket.class, DisconnectPacket::new, "native:disconnect")
        ;
    }

    public static class PacketRegisterBuilder<T extends Packet> {
        private final PacketRegistry<T> registry;

        private PacketRegisterBuilder(PacketRegistry<T> registry) {
            this.registry = registry;
        }

        public PacketTypeBuilderWithFlow of(PacketFlow flow) {
            return new PacketTypeBuilderWithFlow(flow);
        }

        public static <T extends Packet> PacketRegisterBuilder<T> get() {
            return new PacketRegisterBuilder<>(PacketRegistry.get());
        }

        public class PacketTypeBuilderWithFlow {
            private final PacketFlow flow;

            private PacketTypeBuilderWithFlow(PacketFlow flow) {
                this.flow = flow;
            }

            public PacketTypeBuilderWithFlow register(@NotNull Class<? extends T> clazz, @NotNull Supplier<T> creator, @NotNull String name) {
                return register(clazz, creator, SpacedName.parse(name));
            }

            public PacketTypeBuilderWithFlow register(@NotNull Class<? extends T> clazz, @NotNull Supplier<T> creator, @NotNull String space, @NotNull String name) {
                return register(clazz, creator, new SpacedName(space, name));
            }

            public PacketTypeBuilderWithFlow register(@NotNull Class<? extends T> clazz, @NotNull Supplier<T> creator, @NotNull SpacedName id) {
                registry.register(clazz, new PacketType<>(creator, id, flow));
                return this;
            }

            public PacketTypeBuilderWithFlow of(PacketFlow flow) {
                return new PacketTypeBuilderWithFlow(flow);
            }
        }
    }

}
