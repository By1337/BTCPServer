package org.by1337.btcp.common.io;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketRegistry;
import org.by1337.btcp.common.packet.PacketType;
import org.by1337.btcp.common.util.id.SpacedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ByteBuffer extends ByteBuf {
    private final ByteBuf source;

    public ByteBuffer(ByteBuf source) {
        this.source = source;
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

    @CanIgnoreReturnValue
    public ByteBuffer writeUUID(@NotNull UUID uuid) {
        this.writeVarLong(uuid.getMostSignificantBits());
        this.writeVarLong(uuid.getLeastSignificantBits());
        return this;
    }

    public UUID readUUID() {
        return new UUID(this.readVarLong(), this.readVarLong());
    }

    @CanIgnoreReturnValue
    public ByteBuffer writeVarInt(int i) {
        while ((i & -128) != 0) {
            this.writeByte(i & 127 | 128);
            i >>>= 7;
        }

        this.writeByte(i);
        return this;
    }

    @CanIgnoreReturnValue
    public ByteBuffer writeVarLong(long l) {
        while ((l & -128L) != 0L) {
            this.writeByte((int) (l & 127L) | 128);
            l >>>= 7;
        }
        this.writeByte((int) l);
        return this;
    }

    public String readUtf() {
        return readUtf(32767);
    }

    public String readUtf(int len) {
        int realLen = this.readVarInt();
        if (realLen > len * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + realLen + " > " + len * 4 + ")");
        } else if (realLen < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            String var3 = this.toString(this.readerIndex(), realLen, StandardCharsets.UTF_8);
            this.readerIndex(this.readerIndex() + realLen);
            if (var3.length() > len) {
                throw new DecoderException("The received string length is longer than maximum allowed (" + realLen + " > " + len + ")");
            } else {
                return var3;
            }
        }
    }

    @CanIgnoreReturnValue
    public ByteBuffer writeUtf(@NotNull String string) {
        return this.writeUtf(string, 32767);
    }

    @CanIgnoreReturnValue
    public ByteBuffer writeUtf(@NotNull String string, int maxLen) {
        byte[] var3 = string.getBytes(StandardCharsets.UTF_8);
        if (var3.length > maxLen) {
            throw new EncoderException("String too big (was " + var3.length + " bytes encoded, max " + maxLen + ")");
        } else {
            this.writeVarInt(var3.length);
            this.writeBytes(var3);
            return this;
        }
    }

    public <T> void writeList(Collection<T> list, BiConsumer<ByteBuffer, T> consumer) {
        writeVarInt(list.size());
        for (T t : list) {
            consumer.accept(this, t);
        }
    }

    public <T> List<T> readList(Function<ByteBuffer, T> function) {
        int size = readVarInt();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(function.apply(this));
        }
        return list;
    }

    public void writeStringList(Collection<String> list) {
        writeList(list, (ByteBuffer::writeUtf));
    }

    public List<String> readStringList() {
        return readList(ByteBuffer::readUtf);
    }

    public Packet readPacket() throws IOException {
        int id = readVarInt();
        PacketType<?> type = PacketRegistry.get().lookup(id);
        if (type == null) {
            throw new IOException("Bad packet id " + id);
        }
        Packet packet = type.getCreator().get();
        packet.read(this);
        return packet;
    }

    public void writePacket(Packet packet) throws IOException {
        PacketType<?> type = PacketRegistry.get().lookup(packet.getClass());
        if (type == null) {
            throw new IOException("Can't serialize unregistered packet");
        }
        writeVarInt(type.getIdAsInt());
        packet.write(this);
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

    public <K, V> void writeMap(Map<K, V> source, ExceptionBiConsumer<ByteBuffer, K> keySerializer, ExceptionBiConsumer<ByteBuffer, V> valueSerializer) throws IOException {
        writeVarInt(source.size());
        for (Map.Entry<K, V> entry : source.entrySet()) {
            keySerializer.accept(this, entry.getKey());
            valueSerializer.accept(this, entry.getValue());
        }
    }

    public <K, V> Map<K, V> readMap(FunctionException<ByteBuffer, K> keyDeserializer, FunctionException<ByteBuffer, V> valueDeserializer) throws IOException {
        int size = readVarInt();
        Map<K, V> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(keyDeserializer.apply(this), valueDeserializer.apply(this));
        }
        return map;
    }

    public <T> void writeOptional(@Nullable T val, ExceptionBiConsumer<ByteBuffer, T> serializer) throws IOException {
        if (val != null) {
            writeBoolean(true);
            serializer.accept(this, val);
        } else {
            writeBoolean(false);
        }
    }

    public <T> Optional<T> readOptional(FunctionException<ByteBuffer, T> deserializer) throws IOException {
        if (readBoolean()) {
            return Optional.of(deserializer.apply(this));
        } else {
            return Optional.empty();
        }
    }


    /**
     * Returns the number of bytes (octets) this buffer can contain.
     */
    @Override
    public int capacity() {
        return source.capacity();
    }

    /**
     * Adjusts the capacity of this buffer.  If the {@code newCapacity} is less than the current
     * capacity, the content of this buffer is truncated.  If the {@code newCapacity} is greater
     * than the current capacity, the buffer is appended with unspecified data whose length is
     * {@code (newCapacity - currentCapacity)}.
     *
     * @param newCapacity
     * @throws IllegalArgumentException if the {@code newCapacity} is greater than {@link #maxCapacity()}
     */
    @Override
    public ByteBuf capacity(int newCapacity) {
        return source.capacity(newCapacity);
    }

    /**
     * Returns the maximum allowed capacity of this buffer. This value provides an upper
     * bound on {@link #capacity()}.
     */
    @Override
    public int maxCapacity() {
        return source.maxCapacity();
    }

    /**
     * Returns the {@link ByteBufAllocator} which created this buffer.
     */
    @Override
    public ByteBufAllocator alloc() {
        return source.alloc();
    }

    /**
     * Returns the <a href="https://en.wikipedia.org/wiki/Endianness">endianness</a>
     * of this buffer.
     *
     * @deprecated use the Little Endian accessors, e.g. {@code getShortLE}, {@code getIntLE}
     * instead of creating a buffer with swapped {@code endianness}.
     */
    @Override
    public ByteOrder order() {
        return source.order();
    }

    /**
     * Returns a buffer with the specified {@code endianness} which shares the whole region,
     * indexes, and marks of this buffer.  Modifying the content, the indexes, or the marks of the
     * returned buffer or this buffer affects each other's content, indexes, and marks.  If the
     * specified {@code endianness} is identical to this buffer's byte order, this method can
     * return {@code this}.  This method does not modify {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @param endianness
     * @deprecated use the Little Endian accessors, e.g. {@code getShortLE}, {@code getIntLE}
     * instead of creating a buffer with swapped {@code endianness}.
     */
    @Override
    public ByteBuf order(ByteOrder endianness) {
        return source.order(endianness);
    }

    /**
     * Return the underlying buffer instance if this buffer is a wrapper of another buffer.
     *
     * @return {@code null} if this buffer is not a wrapper
     */
    @Override
    public ByteBuf unwrap() {
        return source.unwrap();
    }

    /**
     * Returns {@code true} if and only if this buffer is backed by an
     * NIO direct buffer.
     */
    @Override
    public boolean isDirect() {
        return source.isDirect();
    }

    /**
     * Returns {@code true} if and only if this buffer is read-only.
     */
    @Override
    public boolean isReadOnly() {
        return source.isReadOnly();
    }

    /**
     * Returns a read-only version of this buffer.
     */
    @Override
    public ByteBuf asReadOnly() {
        return source.asReadOnly();
    }

    /**
     * Returns the {@code readerIndex} of this buffer.
     */
    @Override
    public int readerIndex() {
        return source.readerIndex();
    }

    /**
     * Sets the {@code readerIndex} of this buffer.
     *
     * @param readerIndex
     * @throws IndexOutOfBoundsException if the specified {@code readerIndex} is
     *                                   less than {@code 0} or
     *                                   greater than {@code this.writerIndex}
     */
    @Override
    public ByteBuf readerIndex(int readerIndex) {
        return source.readerIndex(readerIndex);
    }

    /**
     * Returns the {@code writerIndex} of this buffer.
     */
    @Override
    public int writerIndex() {
        return source.writerIndex();
    }

    /**
     * Sets the {@code writerIndex} of this buffer.
     *
     * @param writerIndex
     * @throws IndexOutOfBoundsException if the specified {@code writerIndex} is
     *                                   less than {@code this.readerIndex} or
     *                                   greater than {@code this.capacity}
     */
    @Override
    public ByteBuf writerIndex(int writerIndex) {
        return source.writerIndex(writerIndex);
    }

    /**
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer
     * in one shot.  This method is useful when you have to worry about the
     * invocation order of {@link #readerIndex(int)} and {@link #writerIndex(int)}
     * methods.  For example, the following code will fail:
     *
     * <pre>
     * // Create a buffer whose readerIndex, writerIndex and capacity are
     * // 0, 0 and 8 respectively.
     * {@link ByteBuf} buf = {@link io.netty.buffer.Unpooled}.buffer(8);
     *
     * // IndexOutOfBoundsException is thrown because the specified
     * // readerIndex (2) cannot be greater than the current writerIndex (0).
     * buf.readerIndex(2);
     * buf.writerIndex(4);
     * </pre>
     * <p>
     * The following code will also fail:
     *
     * <pre>
     * // Create a buffer whose readerIndex, writerIndex and capacity are
     * // 0, 8 and 8 respectively.
     * {@link ByteBuf} buf = {@link io.netty.buffer.Unpooled}.wrappedBuffer(new byte[8]);
     *
     * // readerIndex becomes 8.
     * buf.readLong();
     *
     * // IndexOutOfBoundsException is thrown because the specified
     * // writerIndex (4) cannot be less than the current readerIndex (8).
     * buf.writerIndex(4);
     * buf.readerIndex(2);
     * </pre>
     * <p>
     * By contrast, this method guarantees that it never
     * throws an {@link IndexOutOfBoundsException} as long as the specified
     * indexes meet basic constraints, regardless what the current index
     * values of the buffer are:
     *
     * <pre>
     * // No matter what the current state of the buffer is, the following
     * // call always succeeds as long as the capacity of the buffer is not
     * // less than 4.
     * buf.setIndex(2, 4);
     * </pre>
     *
     * @param readerIndex
     * @param writerIndex
     * @throws IndexOutOfBoundsException if the specified {@code readerIndex} is less than 0,
     *                                   if the specified {@code writerIndex} is less than the specified
     *                                   {@code readerIndex} or if the specified {@code writerIndex} is
     *                                   greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setIndex(int readerIndex, int writerIndex) {
        return source.setIndex(readerIndex, writerIndex);
    }

    /**
     * Returns the number of readable bytes which is equal to
     * {@code (this.writerIndex - this.readerIndex)}.
     */
    @Override
    public int readableBytes() {
        return source.readableBytes();
    }

    /**
     * Returns the number of writable bytes which is equal to
     * {@code (this.capacity - this.writerIndex)}.
     */
    @Override
    public int writableBytes() {
        return source.writableBytes();
    }

    /**
     * Returns the maximum possible number of writable bytes, which is equal to
     * {@code (this.maxCapacity - this.writerIndex)}.
     */
    @Override
    public int maxWritableBytes() {
        return source.maxWritableBytes();
    }

    /**
     * Returns {@code true}
     * if and only if {@code (this.writerIndex - this.readerIndex)} is greater
     * than {@code 0}.
     */
    @Override
    public boolean isReadable() {
        return source.isReadable();
    }

    /**
     * Returns {@code true} if and only if this buffer contains equal to or more than the specified number of elements.
     *
     * @param size
     */
    @Override
    public boolean isReadable(int size) {
        return source.isReadable(size);
    }

    /**
     * Returns {@code true}
     * if and only if {@code (this.capacity - this.writerIndex)} is greater
     * than {@code 0}.
     */
    @Override
    public boolean isWritable() {
        return source.isWritable();
    }

    /**
     * Returns {@code true} if and only if this buffer has enough room to allow writing the specified number of
     * elements.
     *
     * @param size
     */
    @Override
    public boolean isWritable(int size) {
        return source.isWritable(size);
    }

    /**
     * Sets the {@code readerIndex} and {@code writerIndex} of this buffer to
     * {@code 0}.
     * This method is identical to {@link #setIndex(int, int) setIndex(0, 0)}.
     * <p>
     * Please note that the behavior of this method is different
     * from that of NIO buffer, which sets the {@code limit} to
     * the {@code capacity} of the buffer.
     */
    @Override
    public ByteBuf clear() {
        return source.clear();
    }

    /**
     * Marks the current {@code readerIndex} in this buffer.  You can
     * reposition the current {@code readerIndex} to the marked
     * {@code readerIndex} by calling {@link #resetReaderIndex()}.
     * The initial value of the marked {@code readerIndex} is {@code 0}.
     */
    @Override
    public ByteBuf markReaderIndex() {
        return source.markReaderIndex();
    }

    /**
     * Repositions the current {@code readerIndex} to the marked
     * {@code readerIndex} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the current {@code writerIndex} is less than the marked
     *                                   {@code readerIndex}
     */
    @Override
    public ByteBuf resetReaderIndex() {
        return source.resetReaderIndex();
    }

    /**
     * Marks the current {@code writerIndex} in this buffer.  You can
     * reposition the current {@code writerIndex} to the marked
     * {@code writerIndex} by calling {@link #resetWriterIndex()}.
     * The initial value of the marked {@code writerIndex} is {@code 0}.
     */
    @Override
    public ByteBuf markWriterIndex() {
        return source.markWriterIndex();
    }

    /**
     * Repositions the current {@code writerIndex} to the marked
     * {@code writerIndex} in this buffer.
     *
     * @throws IndexOutOfBoundsException if the current {@code readerIndex} is greater than the marked
     *                                   {@code writerIndex}
     */
    @Override
    public ByteBuf resetWriterIndex() {
        return source.resetWriterIndex();
    }

    /**
     * Discards the bytes between the 0th index and {@code readerIndex}.
     * It moves the bytes between {@code readerIndex} and {@code writerIndex}
     * to the 0th index, and sets {@code readerIndex} and {@code writerIndex}
     * to {@code 0} and {@code oldWriterIndex - oldReaderIndex} respectively.
     * <p>
     * Please refer to the class documentation for more detailed explanation.
     */
    @Override
    public ByteBuf discardReadBytes() {
        return source.discardReadBytes();
    }

    /**
     * Similar to {@link ByteBuf#discardReadBytes()} except that this method might discard
     * some, all, or none of read bytes depending on its internal implementation to reduce
     * overall memory bandwidth consumption at the cost of potentially additional memory
     * consumption.
     */
    @Override
    public ByteBuf discardSomeReadBytes() {
        return source.discardSomeReadBytes();
    }

    /**
     * Expands the buffer {@link #capacity()} to make sure the number of
     * {@linkplain #writableBytes() writable bytes} is equal to or greater than the
     * specified value.  If there are enough writable bytes in this buffer, this method
     * returns with no side effect.
     *
     * @param minWritableBytes the expected minimum number of writable bytes
     * @throws IndexOutOfBoundsException if {@link #writerIndex()} + {@code minWritableBytes} &gt; {@link #maxCapacity()}.
     * @see #capacity(int)
     */
    @Override
    public ByteBuf ensureWritable(int minWritableBytes) {
        return source.ensureWritable(minWritableBytes);
    }

    /**
     * Expands the buffer {@link #capacity()} to make sure the number of
     * {@linkplain #writableBytes() writable bytes} is equal to or greater than the
     * specified value. Unlike {@link #ensureWritable(int)}, this method returns a status code.
     *
     * @param minWritableBytes the expected minimum number of writable bytes
     * @param force            When {@link #writerIndex()} + {@code minWritableBytes} &gt; {@link #maxCapacity()}:
     *                         <ul>
     *                         <li>{@code true} - the capacity of the buffer is expanded to {@link #maxCapacity()}</li>
     *                         <li>{@code false} - the capacity of the buffer is unchanged</li>
     *                         </ul>
     * @return {@code 0} if the buffer has enough writable bytes, and its capacity is unchanged.
     * {@code 1} if the buffer does not have enough bytes, and its capacity is unchanged.
     * {@code 2} if the buffer has enough writable bytes, and its capacity has been increased.
     * {@code 3} if the buffer does not have enough bytes, but its capacity has been
     * increased to its maximum.
     */
    @Override
    public int ensureWritable(int minWritableBytes, boolean force) {
        return source.ensureWritable(minWritableBytes, force);
    }

    /**
     * Gets a boolean at the specified absolute (@code index) in this buffer.
     * This method does not modify the {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 1} is greater than {@code this.capacity}
     */
    @Override
    public boolean getBoolean(int index) {
        return source.getBoolean(index);
    }

    /**
     * Gets a byte at the specified absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 1} is greater than {@code this.capacity}
     */
    @Override
    public byte getByte(int index) {
        return source.getByte(index);
    }

    /**
     * Gets an unsigned byte at the specified absolute {@code index} in this
     * buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 1} is greater than {@code this.capacity}
     */
    @Override
    public short getUnsignedByte(int index) {
        return source.getUnsignedByte(index);
    }

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    @Override
    public short getShort(int index) {
        return source.getShort(index);
    }

    /**
     * Gets a 16-bit short integer at the specified absolute {@code index} in
     * this buffer in Little Endian Byte Order. This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    @Override
    public short getShortLE(int index) {
        return source.getShortLE(index);
    }

    /**
     * Gets an unsigned 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    @Override
    public int getUnsignedShort(int index) {
        return source.getUnsignedShort(index);
    }

    /**
     * Gets an unsigned 16-bit short integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    @Override
    public int getUnsignedShortLE(int index) {
        return source.getUnsignedShortLE(index);
    }

    /**
     * Gets a 24-bit medium integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    @Override
    public int getMedium(int index) {
        return source.getMedium(index);
    }

    /**
     * Gets a 24-bit medium integer at the specified absolute {@code index} in
     * this buffer in the Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    @Override
    public int getMediumLE(int index) {
        return source.getMediumLE(index);
    }

    /**
     * Gets an unsigned 24-bit medium integer at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    @Override
    public int getUnsignedMedium(int index) {
        return source.getUnsignedMedium(index);
    }

    /**
     * Gets an unsigned 24-bit medium integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    @Override
    public int getUnsignedMediumLE(int index) {
        return source.getUnsignedMediumLE(index);
    }

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    @Override
    public int getInt(int index) {
        return source.getInt(index);
    }

    /**
     * Gets a 32-bit integer at the specified absolute {@code index} in
     * this buffer with Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    @Override
    public int getIntLE(int index) {
        return source.getIntLE(index);
    }

    /**
     * Gets an unsigned 32-bit integer at the specified absolute {@code index}
     * in this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    @Override
    public long getUnsignedInt(int index) {
        return source.getUnsignedInt(index);
    }

    /**
     * Gets an unsigned 32-bit integer at the specified absolute {@code index}
     * in this buffer in Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    @Override
    public long getUnsignedIntLE(int index) {
        return source.getUnsignedIntLE(index);
    }

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    @Override
    public long getLong(int index) {
        return source.getLong(index);
    }

    /**
     * Gets a 64-bit long integer at the specified absolute {@code index} in
     * this buffer in Little Endian Byte Order. This method does not
     * modify {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    @Override
    public long getLongLE(int index) {
        return source.getLongLE(index);
    }

    /**
     * Gets a 2-byte UTF-16 character at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    @Override
    public char getChar(int index) {
        return source.getChar(index);
    }

    /**
     * Gets a 32-bit floating point number at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    @Override
    public float getFloat(int index) {
        return source.getFloat(index);
    }

    /**
     * Gets a 64-bit floating point number at the specified absolute
     * {@code index} in this buffer.  This method does not modify
     * {@code readerIndex} or {@code writerIndex} of this buffer.
     *
     * @param index
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    @Override
    public double getDouble(int index) {
        return source.getDouble(index);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index} until the destination becomes
     * non-writable.  This method is basically same with
     * {@link #getBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while
     * {@link #getBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @param index
     * @param dst
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + dst.writableBytes} is greater than
     *                                   {@code this.capacity}
     */
    @Override
    public ByteBuf getBytes(int index, ByteBuf dst) {
        return source.getBytes(index, dst);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.  This method is basically same
     * with {@link #getBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code writerIndex} of the destination by the
     * number of the transferred bytes while
     * {@link #getBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * the source buffer (i.e. {@code this}).
     *
     * @param index
     * @param dst
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code length} is greater than {@code dst.writableBytes}
     */
    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int length) {
        return source.getBytes(index, dst, length);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of both the source (i.e. {@code this}) and the destination.
     *
     * @param index
     * @param dst
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if the specified {@code dstIndex} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code dstIndex + length} is greater than
     *                                   {@code dst.capacity}
     */
    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        return source.getBytes(index, dst, dstIndex, length);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer
     *
     * @param index
     * @param dst
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + dst.length} is greater than
     *                                   {@code this.capacity}
     */
    @Override
    public ByteBuf getBytes(int index, byte[] dst) {
        return source.getBytes(index, dst);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of this buffer.
     *
     * @param index
     * @param dst
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if the specified {@code dstIndex} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code dstIndex + length} is greater than
     *                                   {@code dst.length}
     */
    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        return source.getBytes(index, dst, dstIndex, length);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute {@code index} until the destination's position
     * reaches its limit.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer while the destination's {@code position} will be increased.
     *
     * @param index
     * @param dst
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + dst.remaining()} is greater than
     *                                   {@code this.capacity}
     */
    @Override
    public ByteBuf getBytes(int index, java.nio.ByteBuffer dst) {
        return source.getBytes(index, dst);
    }

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param out
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}
     * @throws IOException               if the specified stream threw an exception during I/O
     */
    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        return source.getBytes(index, out, length);
    }

    /**
     * Transfers this buffer's data to the specified channel starting at the
     * specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param out
     * @param length the maximum number of bytes to transfer
     * @return the actual number of bytes written out to the specified channel
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        return source.getBytes(index, out, length);
    }

    /**
     * Transfers this buffer's data starting at the specified absolute {@code index}
     * to the specified channel starting at the given file position.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer. This method does not modify the channel's position.
     *
     * @param index
     * @param out
     * @param position the file position at which the transfer is to begin
     * @param length   the maximum number of bytes to transfer
     * @return the actual number of bytes written out to the specified channel
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    @Override
    public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
        return source.getBytes(index, out, position, length);
    }

    /**
     * Gets a {@link CharSequence} with the given length at the given index.
     *
     * @param index
     * @param length  the length to read
     * @param charset that should be used
     * @return the sequence
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     */
    @Override
    public CharSequence getCharSequence(int index, int length, Charset charset) {
        return source.getCharSequence(index, length, charset);
    }

    /**
     * Sets the specified boolean at the specified absolute {@code index} in this
     * buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 1} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setBoolean(int index, boolean value) {
        return source.setBoolean(index, value);
    }

    /**
     * Sets the specified byte at the specified absolute {@code index} in this
     * buffer.  The 24 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 1} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setByte(int index, int value) {
        return source.setByte(index, value);
    }

    /**
     * Sets the specified 16-bit short integer at the specified absolute
     * {@code index} in this buffer.  The 16 high-order bits of the specified
     * value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setShort(int index, int value) {
        return source.setShort(index, value);
    }

    /**
     * Sets the specified 16-bit short integer at the specified absolute
     * {@code index} in this buffer with the Little Endian Byte Order.
     * The 16 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setShortLE(int index, int value) {
        return source.setShortLE(index, value);
    }

    /**
     * Sets the specified 24-bit medium integer at the specified absolute
     * {@code index} in this buffer.  Please note that the most significant
     * byte is ignored in the specified value.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setMedium(int index, int value) {
        return source.setMedium(index, value);
    }

    /**
     * Sets the specified 24-bit medium integer at the specified absolute
     * {@code index} in this buffer in the Little Endian Byte Order.
     * Please note that the most significant byte is ignored in the
     * specified value.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 3} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setMediumLE(int index, int value) {
        return source.setMediumLE(index, value);
    }

    /**
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setInt(int index, int value) {
        return source.setInt(index, value);
    }

    /**
     * Sets the specified 32-bit integer at the specified absolute
     * {@code index} in this buffer with Little Endian byte order
     * .
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setIntLE(int index, int value) {
        return source.setIntLE(index, value);
    }

    /**
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setLong(int index, long value) {
        return source.setLong(index, value);
    }

    /**
     * Sets the specified 64-bit long integer at the specified absolute
     * {@code index} in this buffer in Little Endian Byte Order.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setLongLE(int index, long value) {
        return source.setLongLE(index, value);
    }

    /**
     * Sets the specified 2-byte UTF-16 character at the specified absolute
     * {@code index} in this buffer.
     * The 16 high-order bits of the specified value are ignored.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 2} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setChar(int index, int value) {
        return source.setChar(index, value);
    }

    /**
     * Sets the specified 32-bit floating-point number at the specified
     * absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 4} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setFloat(int index, float value) {
        return source.setFloat(index, value);
    }

    /**
     * Sets the specified 64-bit floating-point number at the specified
     * absolute {@code index} in this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param value
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   {@code index + 8} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setDouble(int index, double value) {
        return source.setDouble(index, value);
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer becomes
     * unreadable.  This method is basically same with
     * {@link #setBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code readerIndex} of the source buffer by
     * the number of the transferred bytes while
     * {@link #setBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer (i.e. {@code this}).
     *
     * @param index
     * @param src
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + src.readableBytes} is greater than
     *                                   {@code this.capacity}
     */
    @Override
    public ByteBuf setBytes(int index, ByteBuf src) {
        return source.setBytes(index, src);
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.  This method is basically same
     * with {@link #setBytes(int, ByteBuf, int, int)}, except that this
     * method increases the {@code readerIndex} of the source buffer by
     * the number of the transferred bytes while
     * {@link #setBytes(int, ByteBuf, int, int)} does not.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer (i.e. {@code this}).
     *
     * @param index
     * @param src
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code length} is greater than {@code src.readableBytes}
     */
    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int length) {
        return source.setBytes(index, src, length);
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex}
     * of both the source (i.e. {@code this}) and the destination.
     *
     * @param index
     * @param src
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if the specified {@code srcIndex} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code srcIndex + length} is greater than
     *                                   {@code src.capacity}
     */
    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        return source.setBytes(index, src, srcIndex, length);
    }

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param src
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + src.length} is greater than
     *                                   {@code this.capacity}
     */
    @Override
    public ByteBuf setBytes(int index, byte[] src) {
        return source.setBytes(index, src);
    }

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param src
     * @param srcIndex
     * @param length
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0},
     *                                   if the specified {@code srcIndex} is less than {@code 0},
     *                                   if {@code index + length} is greater than
     *                                   {@code this.capacity}, or
     *                                   if {@code srcIndex + length} is greater than {@code src.length}
     */
    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        return source.setBytes(index, src, srcIndex, length);
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the specified absolute {@code index} until the source buffer's position
     * reaches its limit.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param src
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + src.remaining()} is greater than
     *                                   {@code this.capacity}
     */
    @Override
    public ByteBuf setBytes(int index, java.nio.ByteBuffer src) {
        return source.setBytes(index, src);
    }

    /**
     * Transfers the content of the specified source stream to this buffer
     * starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param in
     * @param length the number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel.
     * {@code -1} if the specified {@link InputStream} reached EOF.
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException               if the specified stream threw an exception during I/O
     */
    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        return source.setBytes(index, in, length);
    }

    /**
     * Transfers the content of the specified source channel to this buffer
     * starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param in
     * @param length the maximum number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel.
     * {@code -1} if the specified channel is closed or it reached EOF.
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        return source.setBytes(index, in, length);
    }

    /**
     * Transfers the content of the specified source channel starting at the given file position
     * to this buffer starting at the specified absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer. This method does not modify the channel's position.
     *
     * @param index
     * @param in
     * @param position the file position at which the transfer is to begin
     * @param length   the maximum number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel.
     * {@code -1} if the specified channel is closed or it reached EOF.
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than {@code this.capacity}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    @Override
    public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
        return source.setBytes(index, in, position, length);
    }

    /**
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the specified
     * absolute {@code index}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     * @throws IndexOutOfBoundsException if the specified {@code index} is less than {@code 0} or
     *                                   if {@code index + length} is greater than {@code this.capacity}
     */
    @Override
    public ByteBuf setZero(int index, int length) {
        return source.setZero(index, length);
    }

    /**
     * Writes the specified {@link CharSequence} at the given {@code index}.
     * The {@code writerIndex} is not modified by this method.
     *
     * @param index    on which the sequence should be written
     * @param sequence to write
     * @param charset  that should be used.
     * @return the written number of bytes.
     * @throws IndexOutOfBoundsException if the sequence at the given index would be out of bounds of the buffer capacity
     */
    @Override
    public int setCharSequence(int index, CharSequence sequence, Charset charset) {
        return source.setCharSequence(index, sequence, charset);
    }

    /**
     * Gets a boolean at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 1}
     */
    @Override
    public boolean readBoolean() {
        return source.readBoolean();
    }

    /**
     * Gets a byte at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 1}
     */
    @Override
    public byte readByte() {
        return source.readByte();
    }

    /**
     * Gets an unsigned byte at the current {@code readerIndex} and increases
     * the {@code readerIndex} by {@code 1} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 1}
     */
    @Override
    public short readUnsignedByte() {
        return source.readUnsignedByte();
    }

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    @Override
    public short readShort() {
        return source.readShort();
    }

    /**
     * Gets a 16-bit short integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    @Override
    public short readShortLE() {
        return source.readShortLE();
    }

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    @Override
    public int readUnsignedShort() {
        return source.readUnsignedShort();
    }

    /**
     * Gets an unsigned 16-bit short integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    @Override
    public int readUnsignedShortLE() {
        return source.readUnsignedShortLE();
    }

    /**
     * Gets a 24-bit medium integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 3}
     */
    @Override
    public int readMedium() {
        return source.readMedium();
    }

    /**
     * Gets a 24-bit medium integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the
     * {@code readerIndex} by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 3}
     */
    @Override
    public int readMediumLE() {
        return source.readMediumLE();
    }

    /**
     * Gets an unsigned 24-bit medium integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 3}
     */
    @Override
    public int readUnsignedMedium() {
        return source.readUnsignedMedium();
    }

    /**
     * Gets an unsigned 24-bit medium integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 3} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 3}
     */
    @Override
    public int readUnsignedMediumLE() {
        return source.readUnsignedMediumLE();
    }

    /**
     * Gets a 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    @Override
    public int readInt() {
        return source.readInt();
    }

    /**
     * Gets a 32-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    @Override
    public int readIntLE() {
        return source.readIntLE();
    }

    /**
     * Gets an unsigned 32-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    @Override
    public long readUnsignedInt() {
        return source.readUnsignedInt();
    }

    /**
     * Gets an unsigned 32-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    @Override
    public long readUnsignedIntLE() {
        return source.readUnsignedIntLE();
    }

    /**
     * Gets a 64-bit integer at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    @Override
    public long readLong() {
        return source.readLong();
    }

    /**
     * Gets a 64-bit integer at the current {@code readerIndex}
     * in the Little Endian Byte Order and increases the {@code readerIndex}
     * by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    @Override
    public long readLongLE() {
        return source.readLongLE();
    }

    /**
     * Gets a 2-byte UTF-16 character at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 2} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 2}
     */
    @Override
    public char readChar() {
        return source.readChar();
    }

    /**
     * Gets a 32-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 4} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 4}
     */
    @Override
    public float readFloat() {
        return Float.intBitsToFloat(readVarInt());
    }

    /**
     * Gets a 64-bit floating point number at the current {@code readerIndex}
     * and increases the {@code readerIndex} by {@code 8} in this buffer.
     *
     * @throws IndexOutOfBoundsException if {@code this.readableBytes} is less than {@code 8}
     */
    @Override
    public double readDouble() {
        return Double.longBitsToDouble(readVarLong());
    }

    /**
     * Transfers this buffer's data to a newly created buffer starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     * The returned buffer's {@code readerIndex} and {@code writerIndex} are
     * {@code 0} and {@code length} respectively.
     *
     * @param length the number of bytes to transfer
     * @return the newly created buffer which contains the transferred bytes
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     */
    @Override
    public ByteBuf readBytes(int length) {
        return source.readBytes(length);
    }

    /**
     * Returns a new slice of this buffer's sub-region starting at the current
     * {@code readerIndex} and increases the {@code readerIndex} by the size
     * of the new slice (= {@code length}).
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     *
     * @param length the size of the new slice
     * @return the newly created slice
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     */
    @Override
    public ByteBuf readSlice(int length) {
        return source.readSlice(length);
    }

    /**
     * Returns a new retained slice of this buffer's sub-region starting at the current
     * {@code readerIndex} and increases the {@code readerIndex} by the size
     * of the new slice (= {@code length}).
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #readSlice(int)}.
     * This method behaves similarly to {@code readSlice(...).retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     *
     * @param length the size of the new slice
     * @return the newly created slice
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     */
    @Override
    public ByteBuf readRetainedSlice(int length) {
        return source.readRetainedSlice(length);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} until the destination becomes
     * non-writable, and increases the {@code readerIndex} by the number of the
     * transferred bytes.  This method is basically same with
     * {@link #readBytes(ByteBuf, int, int)}, except that this method
     * increases the {@code writerIndex} of the destination by the number of
     * the transferred bytes while {@link #readBytes(ByteBuf, int, int)}
     * does not.
     *
     * @param dst
     * @throws IndexOutOfBoundsException if {@code dst.writableBytes} is greater than
     *                                   {@code this.readableBytes}
     */
    @Override
    public ByteBuf readBytes(ByteBuf dst) {
        return source.readBytes(dst);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #readBytes(ByteBuf, int, int)},
     * except that this method increases the {@code writerIndex} of the
     * destination by the number of the transferred bytes (= {@code length})
     * while {@link #readBytes(ByteBuf, int, int)} does not.
     *
     * @param dst
     * @param length
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes} or
     *                                   if {@code length} is greater than {@code dst.writableBytes}
     */
    @Override
    public ByteBuf readBytes(ByteBuf dst, int length) {
        return source.readBytes(dst, length);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param dst
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code dstIndex} is less than {@code 0},
     *                                   if {@code length} is greater than {@code this.readableBytes}, or
     *                                   if {@code dstIndex + length} is greater than
     *                                   {@code dst.capacity}
     */
    @Override
    public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        return source.readBytes(dst, dstIndex, length);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code dst.length}).
     *
     * @param dst
     * @throws IndexOutOfBoundsException if {@code dst.length} is greater than {@code this.readableBytes}
     */
    @Override
    public ByteBuf readBytes(byte[] dst) {
        return source.readBytes(dst);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} and increases the {@code readerIndex}
     * by the number of the transferred bytes (= {@code length}).
     *
     * @param dst
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code dstIndex} is less than {@code 0},
     *                                   if {@code length} is greater than {@code this.readableBytes}, or
     *                                   if {@code dstIndex + length} is greater than {@code dst.length}
     */
    @Override
    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        return source.readBytes(dst, dstIndex, length);
    }

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current {@code readerIndex} until the destination's position
     * reaches its limit, and increases the {@code readerIndex} by the
     * number of the transferred bytes.
     *
     * @param dst
     * @throws IndexOutOfBoundsException if {@code dst.remaining()} is greater than
     *                                   {@code this.readableBytes}
     */
    @Override
    public ByteBuf readBytes(java.nio.ByteBuffer dst) {
        return source.readBytes(dst);
    }

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.
     *
     * @param out
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException               if the specified stream threw an exception during I/O
     */
    @Override
    public ByteBuf readBytes(OutputStream out, int length) throws IOException {
        return source.readBytes(out, length);
    }

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * current {@code readerIndex}.
     *
     * @param out
     * @param length the maximum number of bytes to transfer
     * @return the actual number of bytes written out to the specified channel
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    @Override
    public int readBytes(GatheringByteChannel out, int length) throws IOException {
        return source.readBytes(out, length);
    }

    /**
     * Gets a {@link CharSequence} with the given length at the current {@code readerIndex}
     * and increases the {@code readerIndex} by the given length.
     *
     * @param length  the length to read
     * @param charset that should be used
     * @return the sequence
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     */
    @Override
    public CharSequence readCharSequence(int length, Charset charset) {
        return source.readCharSequence(length, charset);
    }

    /**
     * Transfers this buffer's data starting at the current {@code readerIndex}
     * to the specified channel starting at the given file position.
     * This method does not modify the channel's position.
     *
     * @param out
     * @param position the file position at which the transfer is to begin
     * @param length   the maximum number of bytes to transfer
     * @return the actual number of bytes written out to the specified channel
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     * @throws IOException               if the specified channel threw an exception during I/O
     */
    @Override
    public int readBytes(FileChannel out, long position, int length) throws IOException {
        return source.readBytes(out, position, length);
    }

    /**
     * Increases the current {@code readerIndex} by the specified
     * {@code length} in this buffer.
     *
     * @param length
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     */
    @Override
    public ByteBuf skipBytes(int length) {
        return source.skipBytes(length);
    }

    /**
     * Sets the specified boolean at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 1} in this buffer.
     * If {@code this.writableBytes} is less than {@code 1}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeBoolean(boolean value) {
        return source.writeBoolean(value);
    }

    /**
     * Sets the specified byte at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 1} in this buffer.
     * The 24 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 1}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeByte(int value) {
        return source.writeByte(value);
    }

    /**
     * Sets the specified 16-bit short integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
     * in this buffer.  The 16 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 2}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeShort(int value) {
        return source.writeShort(value);
    }

    /**
     * Sets the specified 16-bit short integer in the Little Endian Byte
     * Order at the current {@code writerIndex} and increases the
     * {@code writerIndex} by {@code 2} in this buffer.
     * The 16 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 2}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeShortLE(int value) {
        return source.writeShortLE(value);
    }

    /**
     * Sets the specified 24-bit medium integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 3}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 3}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeMedium(int value) {
        return source.writeMedium(value);
    }

    /**
     * Sets the specified 24-bit medium integer at the current
     * {@code writerIndex} in the Little Endian Byte Order and
     * increases the {@code writerIndex} by {@code 3} in this
     * buffer.
     * If {@code this.writableBytes} is less than {@code 3}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeMediumLE(int value) {
        return source.writeMediumLE(value);
    }

    /**
     * Sets the specified 32-bit integer at the current {@code writerIndex}
     * and increases the {@code writerIndex} by {@code 4} in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeInt(int value) {
        return source.writeInt(value);
    }

    /**
     * Sets the specified 32-bit integer at the current {@code writerIndex}
     * in the Little Endian Byte Order and increases the {@code writerIndex}
     * by {@code 4} in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeIntLE(int value) {
        return source.writeIntLE(value);
    }

    /**
     * Sets the specified 64-bit long integer at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeLong(long value) {
        return source.writeLong(value);
    }

    /**
     * Sets the specified 64-bit long integer at the current
     * {@code writerIndex} in the Little Endian Byte Order and
     * increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeLongLE(long value) {
        return source.writeLongLE(value);
    }

    /**
     * Sets the specified 2-byte UTF-16 character at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 2}
     * in this buffer.  The 16 high-order bits of the specified value are ignored.
     * If {@code this.writableBytes} is less than {@code 2}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeChar(int value) {
        return source.writeChar(value);
    }

    /**
     * Sets the specified 32-bit floating point number at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 4}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 4}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeFloat(float value) {
        writeVarInt(Float.floatToRawIntBits(value));
        return source;
    }

    /**
     * Sets the specified 64-bit floating point number at the current
     * {@code writerIndex} and increases the {@code writerIndex} by {@code 8}
     * in this buffer.
     * If {@code this.writableBytes} is less than {@code 8}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param value
     */
    @Override
    public ByteBuf writeDouble(double value) {
        writeVarLong((Double.doubleToRawLongBits(value)));
        return source;
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer becomes
     * unreadable, and increases the {@code writerIndex} by the number of
     * the transferred bytes.  This method is basically same with
     * {@link #writeBytes(ByteBuf, int, int)}, except that this method
     * increases the {@code readerIndex} of the source buffer by the number of
     * the transferred bytes while {@link #writeBytes(ByteBuf, int, int)}
     * does not.
     * If {@code this.writableBytes} is less than {@code src.readableBytes},
     * {@link #ensureWritable(int)} will be called in an attempt to expand
     * capacity to accommodate.
     *
     * @param src
     */
    @Override
    public ByteBuf writeBytes(ByteBuf src) {
        return source.writeBytes(src);
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).  This method
     * is basically same with {@link #writeBytes(ByteBuf, int, int)},
     * except that this method increases the {@code readerIndex} of the source
     * buffer by the number of the transferred bytes (= {@code length}) while
     * {@link #writeBytes(ByteBuf, int, int)} does not.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param src
     * @param length the number of bytes to transfer
     * @throws IndexOutOfBoundsException if {@code length} is greater then {@code src.readableBytes}
     */
    @Override
    public ByteBuf writeBytes(ByteBuf src, int length) {
        return source.writeBytes(src, length);
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param src
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code srcIndex} is less than {@code 0}, or
     *                                   if {@code srcIndex + length} is greater than {@code src.capacity}
     */
    @Override
    public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
        return source.writeBytes(src, srcIndex, length);
    }

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code src.length}).
     * If {@code this.writableBytes} is less than {@code src.length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param src
     */
    @Override
    public ByteBuf writeBytes(byte[] src) {
        return source.writeBytes(src);
    }

    /**
     * Transfers the specified source array's data to this buffer starting at
     * the current {@code writerIndex} and increases the {@code writerIndex}
     * by the number of the transferred bytes (= {@code length}).
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param src
     * @param srcIndex the first index of the source
     * @param length   the number of bytes to transfer
     * @throws IndexOutOfBoundsException if the specified {@code srcIndex} is less than {@code 0}, or
     *                                   if {@code srcIndex + length} is greater than {@code src.length}
     */
    @Override
    public ByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        return source.writeBytes(src, srcIndex, length);
    }

    /**
     * Transfers the specified source buffer's data to this buffer starting at
     * the current {@code writerIndex} until the source buffer's position
     * reaches its limit, and increases the {@code writerIndex} by the
     * number of the transferred bytes.
     * If {@code this.writableBytes} is less than {@code src.remaining()},
     * {@link #ensureWritable(int)} will be called in an attempt to expand
     * capacity to accommodate.
     *
     * @param src
     */
    @Override
    public ByteBuf writeBytes(java.nio.ByteBuffer src) {
        return source.writeBytes(src);
    }

    /**
     * Transfers the content of the specified stream to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param in
     * @param length the number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel.
     * {@code -1} if the specified {@link InputStream} reached EOF.
     * @throws IOException if the specified stream threw an exception during I/O
     */
    @Override
    public int writeBytes(InputStream in, int length) throws IOException {
        return source.writeBytes(in, length);
    }

    /**
     * Transfers the content of the specified channel to this buffer
     * starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param in
     * @param length the maximum number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel.
     * {@code -1} if the specified channel is closed or it reached EOF.
     * @throws IOException if the specified channel threw an exception during I/O
     */
    @Override
    public int writeBytes(ScatteringByteChannel in, int length) throws IOException {
        return source.writeBytes(in, length);
    }

    /**
     * Transfers the content of the specified channel starting at the given file position
     * to this buffer starting at the current {@code writerIndex} and increases the
     * {@code writerIndex} by the number of the transferred bytes.
     * This method does not modify the channel's position.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param in
     * @param position the file position at which the transfer is to begin
     * @param length   the maximum number of bytes to transfer
     * @return the actual number of bytes read in from the specified channel.
     * {@code -1} if the specified channel is closed or it reached EOF.
     * @throws IOException if the specified channel threw an exception during I/O
     */
    @Override
    public int writeBytes(FileChannel in, long position, int length) throws IOException {
        return source.writeBytes(in, position, length);
    }

    /**
     * Fills this buffer with <tt>NUL (0x00)</tt> starting at the current
     * {@code writerIndex} and increases the {@code writerIndex} by the
     * specified {@code length}.
     * If {@code this.writableBytes} is less than {@code length}, {@link #ensureWritable(int)}
     * will be called in an attempt to expand capacity to accommodate.
     *
     * @param length the number of <tt>NUL</tt>s to write to the buffer
     */
    @Override
    public ByteBuf writeZero(int length) {
        return source.writeZero(length);
    }

    /**
     * Writes the specified {@link CharSequence} at the current {@code writerIndex} and increases
     * the {@code writerIndex} by the written bytes.
     * in this buffer.
     * If {@code this.writableBytes} is not large enough to write the whole sequence,
     * {@link #ensureWritable(int)} will be called in an attempt to expand capacity to accommodate.
     *
     * @param sequence to write
     * @param charset  that should be used
     * @return the written number of bytes
     */
    @Override
    public int writeCharSequence(CharSequence sequence, Charset charset) {
        return source.writeCharSequence(sequence, charset);
    }

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer. The search takes place from the specified {@code fromIndex}
     * (inclusive) to the specified {@code toIndex} (exclusive).
     * <p>
     * If {@code fromIndex} is greater than {@code toIndex}, the search is
     * performed in a reversed order from {@code fromIndex} (exclusive)
     * down to {@code toIndex} (inclusive).
     * <p>
     * Note that the lower index is always included and higher always excluded.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param fromIndex
     * @param toIndex
     * @param value
     * @return the absolute index of the first occurrence if found.
     * {@code -1} otherwise.
     */
    @Override
    public int indexOf(int fromIndex, int toIndex, byte value) {
        return source.indexOf(fromIndex, toIndex, value);
    }

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search takes place from the current {@code readerIndex}
     * (inclusive) to the current {@code writerIndex} (exclusive).
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param value
     * @return the number of bytes between the current {@code readerIndex}
     * and the first occurrence if found. {@code -1} otherwise.
     */
    @Override
    public int bytesBefore(byte value) {
        return source.bytesBefore(value);
    }

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search starts from the current {@code readerIndex}
     * (inclusive) and lasts for the specified {@code length}.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param length
     * @param value
     * @return the number of bytes between the current {@code readerIndex}
     * and the first occurrence if found. {@code -1} otherwise.
     * @throws IndexOutOfBoundsException if {@code length} is greater than {@code this.readableBytes}
     */
    @Override
    public int bytesBefore(int length, byte value) {
        return source.bytesBefore(length, value);
    }

    /**
     * Locates the first occurrence of the specified {@code value} in this
     * buffer.  The search starts from the specified {@code index} (inclusive)
     * and lasts for the specified {@code length}.
     * <p>
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param length
     * @param value
     * @return the number of bytes between the specified {@code index}
     * and the first occurrence if found. {@code -1} otherwise.
     * @throws IndexOutOfBoundsException if {@code index + length} is greater than {@code this.capacity}
     */
    @Override
    public int bytesBefore(int index, int length, byte value) {
        return source.bytesBefore(index, length, value);
    }

    /**
     * Iterates over the readable bytes of this buffer with the specified {@code processor} in ascending order.
     *
     * @param processor
     * @return {@code -1} if the processor iterated to or beyond the end of the readable bytes.
     * The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */
    @Override
    public int forEachByte(ByteProcessor processor) {
        return source.forEachByte(processor);
    }

    /**
     * Iterates over the specified area of this buffer with the specified {@code processor} in ascending order.
     * (i.e. {@code index}, {@code (index + 1)},  .. {@code (index + length - 1)})
     *
     * @param index
     * @param length
     * @param processor
     * @return {@code -1} if the processor iterated to or beyond the end of the specified area.
     * The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */
    @Override
    public int forEachByte(int index, int length, ByteProcessor processor) {
        return source.forEachByte(index, length, processor);
    }

    /**
     * Iterates over the readable bytes of this buffer with the specified {@code processor} in descending order.
     *
     * @param processor
     * @return {@code -1} if the processor iterated to or beyond the beginning of the readable bytes.
     * The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */
    @Override
    public int forEachByteDesc(ByteProcessor processor) {
        return source.forEachByteDesc(processor);
    }

    /**
     * Iterates over the specified area of this buffer with the specified {@code processor} in descending order.
     * (i.e. {@code (index + length - 1)}, {@code (index + length - 2)}, ... {@code index})
     *
     * @param index
     * @param length
     * @param processor
     * @return {@code -1} if the processor iterated to or beyond the beginning of the specified area.
     * The last-visited index If the {@link ByteProcessor#process(byte)} returned {@code false}.
     */
    @Override
    public int forEachByteDesc(int index, int length, ByteProcessor processor) {
        return source.forEachByteDesc(index, length, processor);
    }

    /**
     * Returns a copy of this buffer's readable bytes.  Modifying the content
     * of the returned buffer or this buffer does not affect each other at all.
     * This method is identical to {@code buf.copy(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     */
    @Override
    public ByteBuf copy() {
        return source.copy();
    }

    /**
     * Returns a copy of this buffer's sub-region.  Modifying the content of
     * the returned buffer or this buffer does not affect each other at all.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param index
     * @param length
     */
    @Override
    public ByteBuf copy(int index, int length) {
        return source.copy(index, length);
    }

    /**
     * Returns a slice of this buffer's readable bytes. Modifying the content
     * of the returned buffer or this buffer affects each other's content
     * while they maintain separate indexes and marks.  This method is
     * identical to {@code buf.slice(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     */
    @Override
    public ByteBuf slice() {
        return source.slice();
    }

    /**
     * Returns a retained slice of this buffer's readable bytes. Modifying the content
     * of the returned buffer or this buffer affects each other's content
     * while they maintain separate indexes and marks.  This method is
     * identical to {@code buf.slice(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice()}.
     * This method behaves similarly to {@code slice().retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     */
    @Override
    public ByteBuf retainedSlice() {
        return source.retainedSlice();
    }

    /**
     * Returns a slice of this buffer's sub-region. Modifying the content of
     * the returned buffer or this buffer affects each other's content while
     * they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Also be aware that this method will NOT call {@link #retain()} and so the
     * reference count will NOT be increased.
     *
     * @param index
     * @param length
     */
    @Override
    public ByteBuf slice(int index, int length) {
        return source.slice(index, length);
    }

    /**
     * Returns a retained slice of this buffer's sub-region. Modifying the content of
     * the returned buffer or this buffer affects each other's content while
     * they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice(int, int)}.
     * This method behaves similarly to {@code slice(...).retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     *
     * @param index
     * @param length
     */
    @Override
    public ByteBuf retainedSlice(int index, int length) {
        return source.retainedSlice(index, length);
    }

    /**
     * Returns a buffer which shares the whole region of this buffer.
     * Modifying the content of the returned buffer or this buffer affects
     * each other's content while they maintain separate indexes and marks.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * The reader and writer marks will not be duplicated. Also be aware that this method will
     * NOT call {@link #retain()} and so the reference count will NOT be increased.
     *
     * @return A buffer whose readable content is equivalent to the buffer returned by {@link #slice()}.
     * However this buffer will share the capacity of the underlying buffer, and therefore allows access to all of the
     * underlying content if necessary.
     */
    @Override
    public ByteBuf duplicate() {
        return source.duplicate();
    }

    /**
     * Returns a retained buffer which shares the whole region of this buffer.
     * Modifying the content of the returned buffer or this buffer affects
     * each other's content while they maintain separate indexes and marks.
     * This method is identical to {@code buf.slice(0, buf.capacity())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     * <p>
     * Note that this method returns a {@linkplain #retain() retained} buffer unlike {@link #slice(int, int)}.
     * This method behaves similarly to {@code duplicate().retain()} except that this method may return
     * a buffer implementation that produces less garbage.
     */
    @Override
    public ByteBuf retainedDuplicate() {
        return source.retainedDuplicate();
    }

    /**
     * Returns the maximum number of NIO {@link ByteBuffer}s that consist this buffer.  Note that {@link #nioBuffers()}
     * or {@link #nioBuffers(int, int)} might return a less number of {@link ByteBuffer}s.
     *
     * @return {@code -1} if this buffer has no underlying {@link ByteBuffer}.
     * the number of the underlying {@link ByteBuffer}s if this buffer has at least one underlying
     * {@link ByteBuffer}.  Note that this method does not return {@code 0} to avoid confusion.
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    @Override
    public int nioBufferCount() {
        return source.nioBufferCount();
    }

    /**
     * Exposes this buffer's readable bytes as an NIO {@link ByteBuffer}. The returned buffer
     * either share or contains the copied content of this buffer, while changing the position
     * and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method is identical to {@code buf.nioBuffer(buf.readerIndex(), buf.readableBytes())}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     * Please note that the returned NIO buffer will not see the changes of this buffer if this buffer
     * is a dynamic buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    @Override
    public java.nio.ByteBuffer nioBuffer() {
        return source.nioBuffer();
    }

    /**
     * Exposes this buffer's sub-region as an NIO {@link ByteBuffer}. The returned buffer
     * either share or contains the copied content of this buffer, while changing the position
     * and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     * Please note that the returned NIO buffer will not see the changes of this buffer if this buffer
     * is a dynamic buffer and it adjusted its capacity.
     *
     * @param index
     * @param length
     * @throws UnsupportedOperationException if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     * @see #nioBufferCount()
     * @see #nioBuffers()
     * @see #nioBuffers(int, int)
     */
    @Override
    public java.nio.ByteBuffer nioBuffer(int index, int length) {
        return source.nioBuffer(index, length);
    }

    /**
     * Internal use only: Exposes the internal NIO buffer.
     *
     * @param index
     * @param length
     */
    @Override
    public java.nio.ByteBuffer internalNioBuffer(int index, int length) {
        return source.internalNioBuffer(index, length);
    }

    /**
     * Exposes this buffer's readable bytes as an NIO {@link ByteBuffer}'s. The returned buffer
     * either share or contains the copied content of this buffer, while changing the position
     * and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer.
     * Please note that the returned NIO buffer will not see the changes of this buffer if this buffer
     * is a dynamic buffer and it adjusted its capacity.
     *
     * @throws UnsupportedOperationException if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     * @see #nioBufferCount()
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     */
    @Override
    public java.nio.ByteBuffer[] nioBuffers() {
        return source.nioBuffers();
    }

    /**
     * Exposes this buffer's bytes as an NIO {@link ByteBuffer}'s for the specified index and length
     * The returned buffer either share or contains the copied content of this buffer, while changing
     * the position and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of this buffer. Please note that the
     * returned NIO buffer will not see the changes of this buffer if this buffer is a dynamic
     * buffer and it adjusted its capacity.
     *
     * @param index
     * @param length
     * @throws UnsupportedOperationException if this buffer cannot create a {@link ByteBuffer} that shares the content with itself
     * @see #nioBufferCount()
     * @see #nioBuffer()
     * @see #nioBuffer(int, int)
     */
    @Override
    public java.nio.ByteBuffer[] nioBuffers(int index, int length) {
        return source.nioBuffers(index, length);
    }

    /**
     * Returns {@code true} if and only if this buffer has a backing byte array.
     * If this method returns true, you can safely call {@link #array()} and
     * {@link #arrayOffset()}.
     */
    @Override
    public boolean hasArray() {
        return source.hasArray();
    }

    /**
     * Returns the backing byte array of this buffer.
     *
     * @throws UnsupportedOperationException if there no accessible backing byte array
     */
    @Override
    public byte[] array() {
        return source.array();
    }

    /**
     * Returns the offset of the first byte within the backing byte array of
     * this buffer.
     *
     * @throws UnsupportedOperationException if there no accessible backing byte array
     */
    @Override
    public int arrayOffset() {
        return source.arrayOffset();
    }

    /**
     * Returns {@code true} if and only if this buffer has a reference to the low-level memory address that points
     * to the backing data.
     */
    @Override
    public boolean hasMemoryAddress() {
        return source.hasMemoryAddress();
    }

    /**
     * Returns the low-level memory address that point to the first byte of ths backing data.
     *
     * @throws UnsupportedOperationException if this buffer does not support accessing the low-level memory address
     */
    @Override
    public long memoryAddress() {
        return source.memoryAddress();
    }

    /**
     * Decodes this buffer's readable bytes into a string with the specified
     * character set name.  This method is identical to
     * {@code buf.toString(buf.readerIndex(), buf.readableBytes(), charsetName)}.
     * This method does not modify {@code readerIndex} or {@code writerIndex} of
     * this buffer.
     *
     * @param charset
     * @throws java.nio.charset.UnsupportedCharsetException if the specified character set name is not supported by the
     *                                                      current VM
     */
    @Override
    public String toString(Charset charset) {
        return source.toString(charset);
    }

    /**
     * Decodes this buffer's sub-region into a string with the specified
     * character set.  This method does not modify {@code readerIndex} or
     * {@code writerIndex} of this buffer.
     *
     * @param index
     * @param length
     * @param charset
     */
    @Override
    public String toString(int index, int length, Charset charset) {
        return source.toString(index, length, charset);
    }

    /**
     * Returns a hash code which was calculated from the content of this
     * buffer.  If there's a byte array which is
     * {@linkplain #equals(Object) equal to} this array, both arrays should
     * return the same value.
     */
    @Override
    public int hashCode() {
        return source.hashCode();
    }

    /**
     * Determines if the content of the specified buffer is identical to the
     * content of this array.  'Identical' here means:
     * <ul>
     * <li>the size of the contents of the two buffers are same and</li>
     * <li>every single byte of the content of the two buffers are same.</li>
     * </ul>
     * Please note that it does not compare {@link #readerIndex()} nor
     * {@link #writerIndex()}.  This method also returns {@code false} for
     * {@code null} and an object which is not an instance of
     * {@link ByteBuf} type.
     *
     * @param obj
     */
    @Override
    public boolean equals(Object obj) {
        return source.equals(obj);
    }

    /**
     * Compares the content of the specified buffer to the content of this
     * buffer. Comparison is performed in the same manner with the string
     * comparison functions of various languages such as {@code strcmp},
     * {@code memcmp} and {@link String#compareTo(String)}.
     *
     * @param buffer
     */
    @Override
    public int compareTo(ByteBuf buffer) {
        return source.compareTo(buffer);
    }

    /**
     * Returns the string representation of this buffer.  This method does not
     * necessarily return the whole content of the buffer but returns
     * the values of the key properties such as {@link #readerIndex()},
     * {@link #writerIndex()} and {@link #capacity()}.
     */
    @Override
    public String toString() {
        return source.toString();
    }

    @Override
    public ByteBuf retain(int increment) {
        return source.retain(increment);
    }

    /**
     * Returns the reference count of this object.  If {@code 0}, it means this object has been deallocated.
     */
    @Override
    public int refCnt() {
        return source.refCnt();
    }

    @Override
    public ByteBuf retain() {
        return source.retain();
    }

    @Override
    public ByteBuf touch() {
        return source.touch();
    }

    @Override
    public ByteBuf touch(Object hint) {
        return source.touch(hint);
    }

    /**
     * Decreases the reference count by {@code 1} and deallocates this object if the reference count reaches at
     * {@code 0}.
     *
     * @return {@code true} if and only if the reference count became {@code 0} and this object has been deallocated
     */
    @Override
    public boolean release() {
        return source.release();
    }

    /**
     * Decreases the reference count by the specified {@code decrement} and deallocates this object if the reference
     * count reaches at {@code 0}.
     *
     * @param decrement
     * @return {@code true} if and only if the reference count became {@code 0} and this object has been deallocated
     */
    @Override
    public boolean release(int decrement) {
        return source.release(decrement);
    }

    @FunctionalInterface
    public interface FunctionException<T, R> {
        R apply(T t) throws IOException;
    }
    @FunctionalInterface
    public interface ExceptionBiConsumer<T, U> {
        void accept(T t, U u) throws IOException;
    }
}
