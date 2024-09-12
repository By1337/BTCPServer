package org.by1337.btcp.client.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.TimeoutException;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketRegistry;
import org.by1337.btcp.common.packet.PacketType;
import org.by1337.btcp.common.packet.impl.DisconnectPacket;
import org.by1337.btcp.common.packet.impl.PacketAuth;
import org.by1337.btcp.common.packet.impl.PacketAuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class ConnectionAuth extends SimpleChannelInboundHandler<Packet> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionAuth.class);
    private final Connection connection;

    public ConnectionAuth(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        PacketAuth auth = new PacketAuth(connection.id, connection.password, connection.secretKey);
        ctx.writeAndFlush(auth);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        if (packet instanceof PacketAuthResponse response) {
            if (response.getResponse() == PacketAuthResponse.Response.SUCCESSFULLY) {
                connection.id = response.getId();
                Channel channel = ctx.channel();
                channel.pipeline().remove("auth");
                channel.pipeline().addLast("handler", connection);
                connection.authorized();
            } else {
                disconnect(ctx, "for no reason");
            }
        } else if (packet instanceof DisconnectPacket disconnectPacket) {
            disconnect(ctx, disconnectPacket.getReason());
        } else {
            PacketType<Packet> type = PacketRegistry.get().lookup(packet.getClass());
            disconnect(ctx, String.format("Invalid response from the server. IN: [%s:(%s)] %s", packet.getClass().getSimpleName(), type == null ? null : type.getId(), packet));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        disconnect(ctx, "End of stream");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        disconnect(ctx, "connection unregister");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof TimeoutException) {
            this.disconnect(ctx, "Timed out");
        } else {
            this.disconnect(ctx, "Internal Exception: " + cause);
            LOGGER.error("An error occurred in the {} connection", ctx.channel().remoteAddress(), cause);
        }
    }


    private void disconnect(ChannelHandlerContext ctx, String message) {
        if (ctx.channel().isOpen()) {
            ctx.channel().close();
            LOGGER.info("Failed to login! Reason: {}", message);
        }
    }
}
