package org.by1337.btcp.common.io.wrapped;

import org.by1337.btcp.common.io.AbstractByteBuffer;

import java.io.ByteArrayOutputStream;

public class WrappedByteArrayOutputStream extends AbstractByteBuffer {
    private final ByteArrayOutputStream buffer;

    public WrappedByteArrayOutputStream(ByteArrayOutputStream buffer) {
        this.buffer = buffer;
    }

    @Override
    public byte readByte() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeByte(byte b) {
        buffer.write(b);
    }

    @Override
    public boolean readBoolean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBoolean(boolean b) {
        buffer.write(b ? 1 : 0);
    }

    @Override
    public void writeBytes(byte[] src) {
        buffer.writeBytes(src);
    }

    @Override
    public byte[] toByteArray() {
        return buffer.toByteArray();
    }
}
