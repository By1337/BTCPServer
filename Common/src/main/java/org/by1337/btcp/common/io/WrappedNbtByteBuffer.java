package org.by1337.btcp.common.io;

import org.by1337.blib.nbt.DefaultNbtByteBuffer;

public class WrappedNbtByteBuffer extends DefaultNbtByteBuffer {
    private final ByteBuffer buffer;

    public WrappedNbtByteBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void writeByte(byte b) {
        buffer.writeByte(b);
    }
    public void writeShort(int value) {
        writeByte((byte) (value >>> 8));
        writeByte((byte) (value));
    }

    public byte readByte() {
        return buffer.readByte();
    }

    public byte[] toByteArray() {
        throw new UnsupportedOperationException();
    }

    public int readableBytes() {
        return buffer.readableBytes();
    }
}
