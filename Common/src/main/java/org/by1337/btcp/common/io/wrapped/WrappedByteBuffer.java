package org.by1337.btcp.common.io.wrapped;

import org.by1337.btcp.common.io.AbstractByteBuffer;
import org.by1337.btcp.common.io.ByteBuffer;

public class WrappedByteBuffer extends AbstractByteBuffer {
    private final ByteBuffer buffer;

    public WrappedByteBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public byte readByte() {
        return buffer.readByte();
    }

    @Override
    public byte[] toByteArray() {
        return new byte[0];
    }

    @Override
    public void writeByte(byte b) {
        buffer.writeByte(b);
    }

    @Override
    public boolean readBoolean() {
        return buffer.readBoolean();
    }

    @Override
    public void writeBoolean(boolean b) {
        buffer.writeBoolean(b);
    }

    @Override
    public void writeBytes(byte[] src) {
        buffer.writeBytes(src);
    }
}
