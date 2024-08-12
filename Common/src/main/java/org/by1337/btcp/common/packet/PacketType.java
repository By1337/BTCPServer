package org.by1337.btcp.common.packet;

import org.by1337.btcp.common.util.id.SpacedName;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

public final class PacketType<T extends Packet> {
    private final Supplier<T> creator;
    private final SpacedName id;
    private final PacketFlow flow;
    private final int intId;

    public PacketType(@NotNull Supplier<T> creator, @NotNull SpacedName id, @NotNull PacketFlow flow) {
        this.creator = creator;
        this.id = id;
        this.flow = flow;
        intId = id.getAsInt();
    }

    public static <T extends Packet> Builder<T> builder() {
        return new Builder<>();
    }

    @NotNull
    public Supplier<T> getCreator() {
        return creator;
    }

    @NotNull
    public SpacedName getId() {
        return id;
    }

    @NotNull
    public PacketFlow getFlow() {
        return flow;
    }

    public int getIdAsInt() {
        return intId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketType<?> that = (PacketType<?>) o;
        return Objects.equals(creator, that.creator) && Objects.equals(id, that.id) && flow == that.flow;
    }

    @Override
    public int hashCode() {
        return Objects.hash(creator, id, flow);
    }

    @Override
    public String toString() {
        return "PacketType{" +
               "creator=" + creator +
               ", id=" + id +
               ", flow=" + flow +
               ", intId=" + intId +
               '}';
    }

    public static class Builder<T extends Packet> {
        private Supplier<T> creator;
        private SpacedName id;
        private PacketFlow flow = PacketFlow.ANY;

        private Builder<T> creator(Supplier<T> creator) {
            this.creator = creator;
            return this;
        }

        public Builder<T> id(SpacedName id) {
            this.id = id;
            return this;
        }

        public Builder<T> flow(PacketFlow flow) {
            this.flow = flow;
            return this;
        }

        public PacketType<T> build() {
            return new PacketType<>(
                    Objects.requireNonNull(creator, "creator is null!"),
                    Objects.requireNonNull(id, "id is null!"),
                    Objects.requireNonNull(flow, "flow is null!")
            );
        }
    }
}
