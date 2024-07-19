package org.by1337.btcp.server.network.channel;

import org.by1337.btcp.client.network.Connection;
import org.by1337.btcp.client.network.channel.ClientChannelManager;
import org.by1337.btcp.client.network.channel.impl.MainClientChannel;
import org.by1337.btcp.common.event.EventManager;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.PacketPingRequest;
import org.by1337.btcp.common.packet.impl.PacketPingResponse;
import org.by1337.btcp.common.packet.impl.channel.ChannelStatusPacket;
import org.by1337.btcp.common.packet.impl.channel.OpenChannelPacket;
import org.by1337.btcp.common.util.id.SpacedName;
import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.by1337.btcp.server.dedicated.client.Client;
import org.by1337.btcp.server.network.channel.impl.MainServerChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerChannelManagerTest {
    private static Logger LOGGER = LoggerFactory.getLogger(ServerChannelManagerTest.class);
    private ServerChannelManager serverChannelManager;
    @Mock
    private DedicatedServer server;
    @Mock
    private Client serverClient;
    private ClientChannelManager clientChannelManager;
    @Mock
    private Connection connection;
    private AutoCloseable closeable;
    private Waiter toServer = new Waiter();
    private Waiter toClient = new Waiter();
    private MainServerChannel serverChannel;
    private MainClientChannel clientChannel;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(server.getEventManager()).thenReturn(new EventManager());
        doAnswer(invocation -> {
            toServerSend(invocation.getArgument(0));
            return null;
        }).when(connection).send(any(Packet.class));
        doAnswer(invocation -> {
            toClientSend(invocation.getArgument(0));
            return null;
        }).when(serverClient).send(any(Packet.class));
        when(serverClient.getId()).thenReturn("testClient");
        serverChannelManager = new ServerChannelManager(server);
        clientChannelManager = new ClientChannelManager(connection);
        serverChannel = new MainServerChannel(serverChannelManager, new SpacedName("test", "main"));
        serverChannel.register();
        clientChannel = new MainClientChannel(clientChannelManager, new SpacedName("test", "main"));

        clientChannelRegisterTest();
    }


    void clientChannelRegisterTest() {
        scheduler.schedule(() -> clientChannel.register(), 5, TimeUnit.MILLISECONDS);
        toServer.sync(20);
        assertTrue(toServer.data instanceof OpenChannelPacket);
        toServer.waitEnd(20);
        assertEquals(ChannelStatusPacket.ChannelStatus.OPENED, clientChannel.getStatus());
    }

    @Test
    void requestToClientTest() {
        LOGGER.info("requestToClientTest");
        Waiter pingResponseWaiter = new Waiter();
        scheduler.schedule(() -> {
            ChanneledClient client = new ChanneledClient(serverClient, serverChannel);
            client.request(new PacketPingRequest(System.currentTimeMillis()), 15, TimeUnit.MILLISECONDS).thenAccept(packetOpt -> {
                pingResponseWaiter.update(packetOpt.orElse(null));
            });
        }, 5, TimeUnit.MILLISECONDS);
        pingResponseWaiter.sync(30);
        assertTrue(pingResponseWaiter.data instanceof PacketPingResponse);
        pingResponseWaiter.waitEnd(30);
    }
    @Test
    void requestToServerTest() {
        LOGGER.info("requestToServerTest");
        Waiter pingResponseWaiter = new Waiter();
        clientChannel.request(new PacketPingRequest(System.currentTimeMillis()), 15, TimeUnit.MILLISECONDS).thenAccept(packetOpt -> {
            pingResponseWaiter.update(packetOpt.orElse(null));
        });
        pingResponseWaiter.sync(30);
        assertTrue(pingResponseWaiter.data instanceof PacketPingResponse);
        pingResponseWaiter.waitEnd(30);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        scheduler.close();
    }

    private void toServerSend(Packet packet) {
        LOGGER.info("TO SERVER {}", packet);
        toServer.update(packet);
        serverChannelManager.onPacket(packet, serverClient);
        toServer.end();
    }

    private void toClientSend(Packet packet) {
        LOGGER.info("TO CLIENT {}", packet);
        toClient.update(packet);
        clientChannelManager.onPacket(packet);
        toClient.end();
    }

    public static class Waiter {
        private final Object updateStatusLock = new Object();
        private final Object endLock = new Object();
        public Packet data;

        public void sync() {
            sync(0L);
        }

        public void sync(long timeoutMillis) {
            try {
                synchronized (updateStatusLock) {
                    updateStatusLock.wait(timeoutMillis);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        void update(Packet data) {
            this.data = data;
            synchronized (updateStatusLock) {
                updateStatusLock.notifyAll();
            }
        }
        public void waitEnd(long timeoutMillis) {
            try {
                synchronized (endLock) {
                    endLock.wait(timeoutMillis);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        void end(){
            synchronized (endLock){
                endLock.notifyAll();
            }
        }
    }
}