package org.by1337.btcp.server.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.DisconnectPacket;
import org.by1337.btcp.common.packet.impl.PacketAuth;
import org.by1337.btcp.common.packet.impl.PacketAuthResponse;
import org.by1337.btcp.server.dedicated.DedicatedServer;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.Optional;

public class UnregisteredConnection extends SimpleChannelInboundHandler<Packet> {
    private final DedicatedServer server;
    private final String password;

    public UnregisteredConnection(DedicatedServer server, String password) {
        this.server = server;
        this.password = password;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        if (packet instanceof PacketAuth auth) {
            if (Objects.equals(password, auth.tryDecodePassword(auth.getPassword()))) {
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
                server.getClientList().newClient(connection);
                connection.send(new PacketAuthResponse(PacketAuthResponse.Response.SUCCESSFULLY));
            } else {
                disconnect(ctx, "Wrong password!");
            }
        } else {
            disconnect(ctx, "Unauthorized!");
        }
    }

    private void disconnect(ChannelHandlerContext ctx, String message) {
        DisconnectPacket packet1 = new DisconnectPacket(message);
        ctx.channel().writeAndFlush(packet1);
        ctx.channel().close();
    }
}
