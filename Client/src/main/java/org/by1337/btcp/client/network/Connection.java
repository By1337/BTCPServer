package org.by1337.btcp.client.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.TimeoutException;
import org.by1337.btcp.common.codec.*;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.PacketFlow;
import org.by1337.btcp.common.packet.impl.DisconnectPacket;
import org.by1337.btcp.common.packet.impl.PacketAuthResponse;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Consumer;

public class Connection extends SimpleChannelInboundHandler<Packet> {
    private final Logger logger;
    private final String ip;
    private final int port;
    @Nullable String id;
    final String password;
    final String secretKey;
    private ChannelFuture channelFuture;
    private EventLoopGroup loopGroup;
    private Channel channel;
    private boolean running;
    private boolean authorized;
    private final ConnectionStatusListener statusListener;

    public Connection(Logger logger, String ip, int port, @Nullable String id, String password, String secretKey, ConnectionStatusListener statusListener) {
        this.logger = logger;
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.password = password;
        this.secretKey = secretKey;
        this.statusListener = statusListener;
    }

    public void start(boolean debug) {
        if (running) {
            throw new IllegalStateException("client already started");
        }
        running = true;
        Class<? extends SocketChannel> channelClass;
        if (Epoll.isAvailable()) {
            channelClass = EpollSocketChannel.class;
            loopGroup = new EpollEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build());
            logger.info("Using epoll channel type");
        } else {
            channelClass = NioSocketChannel.class;
            loopGroup = new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Client IO #%d").setDaemon(true).build());
            logger.info("Using default channel type");
        }
        try {
            channelFuture = new Bootstrap()
                    .channel(channelClass)
                    .handler(
                            new ChannelInitializer<>() {
                                @Override
                                protected void initChannel(Channel channel) {
                                    try {
                                        channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                                    } catch (ChannelException ignore) {
                                    }
                                    channel.pipeline()
                                            .addLast("splitter", new Varint21FrameDecoder())
                                            .addLast("decoder", new PacketDecoder(debug, PacketFlow.CLIENT_BOUND))
                                            .addLast("prepender", new Varint21LengthFieldPrepender())
                                            .addLast("encoder", new PacketEncoder(debug, PacketFlow.SERVER_BOUND))
                                            .addLast("auth", new ConnectionAuth(Connection.this));
                                }
                            }
                    )
                    .group(loopGroup)
                    .connect(ip, port).syncUninterruptibly()
                    .channel()
                    .closeFuture();
            channel = channelFuture.channel();
        } catch (Exception e) {
            logger.error("failed connect", e);
            shutdown();
            throw e;
        }
    }

    public void send(Packet packet) {
        if (channel.isOpen()) {
            channel.writeAndFlush(packet);
        }
    }

    public void shutdown() {
        if (!running) return;
        statusListener.onDisconnect();
        running = false;
        authorized = false;
        logger.info("client shutdown");
        synchronized (this) {
            notifyAll();
        }
        try {
            if (channelFuture != null) {
                channelFuture.channel().close().sync();
                channelFuture = null;
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted whilst closing channel");
        } finally {
            if (loopGroup != null) {
                loopGroup.shutdownGracefully();
                loopGroup = null;
            }
        }
    }

    void authorized() {
        synchronized (this) {
            authorized = true;
            notifyAll();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet packet) throws Exception {
        try {
            statusListener.onPacket(packet);
        } finally {
            packet.release();
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
            logger.error("An error occurred", cause);
        }
    }

    public void setupCompression(int lvl, int threshold) {
        removeCompression();
        channel.pipeline().addBefore("decoder", "decompress", new CompressionDecoder(threshold));
        this.channel.pipeline().addBefore("encoder", "compress", new CompressionEncoder(threshold, lvl));
    }

    private void removeCompression() {
        removeHandler("decompress", h -> {
            if (h instanceof CompressionDecoder decoder) {
                decoder.release();
            }
        });
        removeHandler("compress", h -> {
            if (h instanceof CompressionEncoder encoder) {
                encoder.release();
            }
        });
    }

    private void removeHandler(String name, Consumer<ChannelHandler> action) {
        ChannelHandler decompress = channel.pipeline().get(name);
        if (decompress != null) {
            channel.pipeline().remove(decompress);
            action.accept(decompress);
        }
    }

    private void disconnect(ChannelHandlerContext ctx, String message) {
        ctx.channel().close();
        shutdown();
        logger.info("Disconnected reason: {}", message);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isAuthorized() {
        return authorized;
    }
}
