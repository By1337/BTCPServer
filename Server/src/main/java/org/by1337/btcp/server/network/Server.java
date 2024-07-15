package org.by1337.btcp.server.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.ResourceLeakDetector;
import org.by1337.btcp.common.codec.PacketDecoder;
import org.by1337.btcp.common.codec.PacketEncoder;
import org.by1337.btcp.common.codec.Varint21FrameDecoder;
import org.by1337.btcp.common.codec.Varint21LengthFieldPrepender;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private final int port;
    private final String password;
    private EventLoopGroup loopGroup;
    private ChannelFuture channelFuture;
    private final DedicatedServer server;
    public Server(int port, String password, DedicatedServer server) {
        this.port = port;
        this.password = password;
        this.server = server;
    }

    public void start(boolean debug) {
        if (debug) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        } else {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.SIMPLE);
        }
        Class<? extends ServerSocketChannel> channelClass;
        if (Epoll.isAvailable()) {
            channelClass = EpollServerSocketChannel.class;
            loopGroup = new EpollEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Epoll Server IO #%d").setDaemon(true).build());
            LOGGER.info("Using epoll channel type");
        } else {
            channelClass = NioServerSocketChannel.class;
            loopGroup = new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Server IO #%d").setDaemon(true).build());
            LOGGER.info("Using default channel type");
        }
        channelFuture = new ServerBootstrap()
                .channel(channelClass)
                .childHandler(
                        new ChannelInitializer<>() {
                            @Override
                            protected void initChannel(Channel channel) {
                                try {
                                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                                } catch (ChannelException ignore) {
                                }
                                channel.pipeline()
                                        .addLast("timeout", new ReadTimeoutHandler(30))
                                        .addLast("splitter", new Varint21FrameDecoder())
                                        .addLast("decoder", new PacketDecoder(debug, PacketFlow.SERVER_BOUND))
                                        .addLast("prepender", new Varint21LengthFieldPrepender())
                                        .addLast("encoder", new PacketEncoder(debug, PacketFlow.CLIENT_BOUND))
                                        .addLast("auth", new UnregisteredConnection(server, password));
                            }
                        }
                )
                .group(loopGroup)
                .bind(port)
                .syncUninterruptibly()
        ;
    }
    public void stop() {
        try {
            channelFuture.channel().close().sync();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted whilst closing channel");
            loopGroup.close();
        }
    }
}
