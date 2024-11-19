package org.by1337.btcp.common.packet;

import org.by1337.btcp.common.io.ByteBuffer;

import java.io.IOException;

public abstract class Packet {
    public abstract void read(ByteBuffer byteBuf) throws IOException;

    public abstract void write(ByteBuffer byteBuf) throws IOException;

    public void release() {

    }
}
