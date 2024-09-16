package org.by1337.btcp.common.io.wrapped;

import org.by1337.btcp.common.io.AbstractByteBuffer;

import java.io.ByteArrayInputStream;

public class WrappedByteArrayInputStream extends AbstractByteBuffer {
    private final ByteArrayInputStream in;

    public WrappedByteArrayInputStream(ByteArrayInputStream in) {
        this.in = in;
    }

    @Override
    public byte readByte() {
        return (byte) in.read();
    }

    @Override
    public void writeByte(byte b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean readBoolean() {
        return in.read() != 0;
    }

    @Override
    public void writeBoolean(boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBytes(byte[] src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] toByteArray() {
        throw new UnsupportedOperationException();
    }
}
