package org.by1337.btcp.server.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.TimeoutException;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.DisconnectPacket;
import org.by1337.btcp.common.packet.impl.PacketAuth;
import org.by1337.btcp.common.packet.impl.PacketAuthResponse;
import org.by1337.btcp.common.util.crypto.AESUtil;
import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.by1337.btcp.server.event.ClientLoginEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.Optional;

public class UnregisteredConnection extends SimpleChannelInboundHandler<Packet> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnregisteredConnection.class);
    private final DedicatedServer server;
    private final String password;

    public UnregisteredConnection(DedicatedServer server, String password) {
        this.server = server;
        this.password = password;
    }


    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        try {
            read(ctx, packet);
        } catch (Throwable cause) {
            disconnect(ctx, "Internal Exception: " + cause);
            LOGGER.error("Failed to process the packet from {}", ctx.channel().remoteAddress(), cause);
        }
    }

    protected void read(ChannelHandlerContext ctx, Packet packet) throws Exception {
        ClientLoginEvent loginEvent = new ClientLoginEvent(ctx, packet);
        if (server.getEventManager().callEvent(loginEvent).isCanceled()) {
            String reason = Objects.requireNonNullElse(loginEvent.getReason(), "for no reason");
            disconnect(ctx, reason);
            return;
        }
        if (packet instanceof PacketAuth auth) {
            if (password.equals(AESUtil.decrypt(auth.getPassword(), server.getConfig().getSecretKey()))) {
                Optional<String> optId = auth.getId();
                String id;
                if (optId.isPresent()) {
                    id = optId.get();
                } else {
                    if (!server.getConfig().isAllowAnonymousClients()) {
                        disconnect(ctx, "No anonymous clients allowed!");
                        return;
                    }
                    id = server.getClientList().nextId();
                }
                if (server.getClientList().hasClient(id)) {
                    disconnect(ctx, String.format("The client named %s is already connected!", id));
                    return;
                }
                Channel channel = ctx.channel();
                SocketAddress address = channel.remoteAddress();
                channel.pipeline().remove("timeout");
                channel.pipeline().remove("auth");

                Connection connection = new Connection(channel, address, id, server);
                channel.pipeline().addLast("handler", connection);

                server.getClientList().newClient(connection);
                connection.send(new PacketAuthResponse(
                        PacketAuthResponse.Response.SUCCESSFULLY,
                        id,
                        server.getConfig().getThreshold(),
                        server.getConfig().getCompressionLvl()
                ));
                connection.setupCompression(
                        server.getConfig().getCompressionLvl(),
                        server.getConfig().getThreshold()
                );
            } else {
                disconnect(ctx, "Wrong password!");
            }
        } else {
            disconnect(ctx, "Unauthorized!");
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
            DisconnectPacket packet1 = new DisconnectPacket(message);
            ctx.channel().writeAndFlush(packet1);

            ctx.channel().close();
            LOGGER.info("Disconnect unauthorized connection {}, reason: {}", ctx.channel().remoteAddress(), message);
        }
    }
}
