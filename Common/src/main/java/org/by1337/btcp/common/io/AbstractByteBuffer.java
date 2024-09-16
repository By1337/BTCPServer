package org.by1337.btcp.common.io;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import org.by1337.blib.nbt.NbtByteBuffer;
import org.by1337.btcp.common.io.wrapped.WrappedByteArrayInputStream;
import org.by1337.btcp.common.io.wrapped.WrappedByteArrayOutputStream;
import org.by1337.btcp.common.io.wrapped.WrappedByteBuffer;
import org.by1337.btcp.common.util.id.SpacedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class AbstractByteBuffer implements NbtByteBuffer {

    public static AbstractByteBuffer wrap(ByteBuffer buffer) {
        return new WrappedByteBuffer(buffer);
    }

    public static AbstractByteBuffer wrap(ByteBuf buffer) {
        return new WrappedByteBuffer(new ByteBuffer(buffer));
    }

    public static AbstractByteBuffer wrap(ByteArrayOutputStream buffer) {
        return new WrappedByteArrayOutputStream(buffer);
    }

    public static AbstractByteBuffer wrap(ByteArrayInputStream buffer) {
        return new WrappedByteArrayInputStream(buffer);
    }

    @Override
    public short readShort() {
        int ch1 = readByte();
        int ch2 = readByte();
        return (short) ((ch1 << 8) + (ch2));
    }

    @Override
    public void writeShort(int value) {
        writeByte((byte) (value >>> 8));
        writeByte((byte) (value));
    }

    @Override
    public void writeDouble(double d) {
        writeVarLong((Double.doubleToRawLongBits(d)));
    }

    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readVarLong());
    }

    @Override
    public void writeFloat(float f) {
        writeVarInt(Float.floatToRawIntBits(f));
    }

    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readVarInt());
    }

    public int readVarInt() {
        int result = 0;
        int offsets = 0;

        byte b;
        do {
            b = readByte();
            result |= (b & 127) << offsets++ * 7;
            if (offsets > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b & 128) == 128);

        return result;
    }

    public long readVarLong() {
        long result = 0L;
        int offsets = 0;

        byte b;
        do {
            b = readByte();
            result |= (long) (b & 127) << offsets++ * 7;
            if (offsets > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while ((b & 128) == 128);

        return result;
    }

    public static int getVarIntSize(int i) {
        for (int var1 = 1; var1 < 5; ++var1) {
            if ((i & -1 << var1 * 7) == 0) {
                return var1;
            }
        }
        return 5;
    }

    public void writeUUID(@NotNull UUID uuid) {
        this.writeVarLong(uuid.getMostSignificantBits());
        this.writeVarLong(uuid.getLeastSignificantBits());
    }

    public UUID readUUID() {
        return new UUID(this.readVarLong(), this.readVarLong());
    }


    public void writeVarInt(int i) {
        while ((i & -128) != 0) {
            this.writeByte(i & 127 | 128);
            i >>>= 7;
        }

        this.writeByte(i);
    }

    @CanIgnoreReturnValue
    public void writeVarLong(long l) {
        while ((l & -128L) != 0L) {
            this.writeByte((int) (l & 127L) | 128);
            l >>>= 7;
        }
        this.writeByte((int) l);
    }

    public String readUtf() {
        return readUtf(32767);
    }

    public String readUtf(int len) {
        int realLen = this.readVarInt();
        if (realLen > len << 2) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + realLen + " > " + (len << 2) + ")");
        } else if (realLen < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            byte[] bytes = new byte[realLen];
            readBytes(bytes);
            String decodedString = new String(bytes, StandardCharsets.UTF_8);
            if (decodedString.length() > len) {
                throw new DecoderException("The received string length is longer than maximum allowed (" + realLen + " > " + len + ")");
            } else {
                return decodedString;
            }
        }
    }

    public void writeUtf(@NotNull String string) {
        writeUtf(string, 32767);
    }

    public void writeUtf(@NotNull String string, int maxLen) {
        byte[] var3 = string.getBytes(StandardCharsets.UTF_8);
        if (var3.length > maxLen) {
            throw new EncoderException("String too big (was " + var3.length + " bytes encoded, max " + maxLen + ")");
        } else {
            this.writeVarInt(var3.length);
            this.writeBytes(var3);
        }
    }

    public <T> void writeList(Collection<T> list, BiConsumer<AbstractByteBuffer, T> consumer) {
        writeVarInt(list.size());
        for (T t : list) {
            consumer.accept(this, t);
        }
    }

    public <T> List<T> readList(Function<AbstractByteBuffer, T> function) {
        int size = readVarInt();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(function.apply(this));
        }
        return list;
    }

    public void writeStringList(Collection<String> list) {
        writeList(list, (AbstractByteBuffer::writeUtf));
    }

    public List<String> readStringList() {
        return readList(AbstractByteBuffer::readUtf);
    }

    public void writeSpacedName(SpacedName spacedNameKey) {
        writeUtf(spacedNameKey.asString());
    }

    public SpacedName readSpacedName() {
        return SpacedName.parse(readUtf());
    }

    public void writeEnum(Enum<?> e) {
        this.writeVarInt(e.ordinal());
    }

    public <T extends Enum<T>> T readEnum(Class<T> clazz) {
        return clazz.getEnumConstants()[readVarInt()];
    }

    public <K, V> void writeMap(Map<K, V> source, ThrowingBiConsumer<AbstractByteBuffer, K> keySerializer, ThrowingBiConsumer<AbstractByteBuffer, V> valueSerializer) throws IOException {
        writeVarInt(source.size());
        for (Map.Entry<K, V> entry : source.entrySet()) {
            keySerializer.accept(this, entry.getKey());
            valueSerializer.accept(this, entry.getValue());
        }
    }

    public <K, V> Map<K, V> readMap(ThrowingFunction<AbstractByteBuffer, K> keyDeserializer, ThrowingFunction<AbstractByteBuffer, V> valueDeserializer) throws IOException {
        int size = readVarInt();
        Map<K, V> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(keyDeserializer.apply(this), valueDeserializer.apply(this));
        }
        return map;
    }

    public <T> void writeOptional(@Nullable T val, ThrowingBiConsumer<AbstractByteBuffer, T> serializer) throws IOException {
        if (val != null) {
            writeBoolean(true);
            serializer.accept(this, val);
        } else {
            writeBoolean(false);
        }
    }

    public <T> Optional<T> readOptional(ThrowingFunction<AbstractByteBuffer, T> deserializer) throws IOException {
        if (readBoolean()) {
            return Optional.of(deserializer.apply(this));
        } else {
            return Optional.empty();
        }
    }

    public void readBytes(byte[] arr) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = readByte();
        }
    }

    public void writeByte(int b) {
        writeByte((byte) b);
    }

    public abstract byte readByte();

    public abstract void writeByte(byte b);

    public abstract boolean readBoolean();

    public abstract void writeBoolean(boolean b);

    public abstract void writeBytes(byte[] src);

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws IOException;
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, U> {
        void accept(T t, U u) throws IOException;
    }
}
