package org.by1337.btcp.server.dedicated;

import org.by1337.btcp.common.event.EventManager;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.EncryptedPacket;
import org.by1337.btcp.common.util.id.SpacedName;
import org.by1337.btcp.server.service.ServiceLoader;
import org.by1337.btcp.server.commands.CommandManager;
import org.by1337.btcp.server.console.TcpConsole;
import org.by1337.btcp.server.dedicated.client.Client;
import org.by1337.btcp.server.dedicated.client.ClientList;
import org.by1337.btcp.server.network.Server;
import org.by1337.btcp.server.network.channel.ServerChannelManager;
import org.by1337.btcp.server.network.channel.impl.MainServerChannel;
import org.by1337.btcp.server.util.OptionParser;
import org.by1337.btcp.server.util.resource.ResourceUtil;
import org.by1337.btcp.server.util.time.TimeCounter;
import org.by1337.btcp.server.yaml.YamlContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class DedicatedServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DedicatedServer.class);
    private final boolean debug;
    private final String password;
    private final int port;
    private final Config config;
    private final ClientList clientList;
    private final EventManager eventManager;
    private final Server server;
    private final ServerChannelManager serverChannelManager;
    private final CommandManager commandManager;
    private volatile boolean stopped;
    private final ServiceLoader serviceLoader;

    public DedicatedServer(OptionParser parser) throws YamlContext.YamlParserException, IOException {
        TimeCounter timeCounter = new TimeCounter();

        YamlContext context = new YamlContext(ResourceUtil.saveResource("config.yml", false, new File("./")));
        debug = Boolean.parseBoolean(parser.getOrDefault("debug", context.get("server.debug").getAsString()));
        password = parser.getOrDefault("password", context.get("server.password").getAsString());
        port = Integer.parseInt(parser.getOrDefault("port", context.get("server.port").getAsString()));
        System.setProperty("io.netty.eventLoopThreads", context.get("server.netty-threads").getAsString("6"));
        System.setProperty("io.netty.eventLoopThreads", context.get("server.netty-eventLoopThreads").getAsString("6"));
        config = new Config(context);
        EncryptedPacket.setSecretKey(config.getSecretKey());
        clientList = new ClientList();
        eventManager = new EventManager();
        serverChannelManager = new ServerChannelManager(this);
        new MainServerChannel(serverChannelManager, new SpacedName("native", "main")).register();
        commandManager = new CommandManager(this);

        serviceLoader = new ServiceLoader(new File("./services"), this);
        serviceLoader.onLoadPingAll();

        server = new Server(port, password, this);
        server.start(debug);

        serviceLoader.enableAll();
        LOGGER.info("Done in ({})", timeCounter.getTimeFormat());

        TcpConsole tcpConsole = new TcpConsole(this, commandManager);
        tcpConsole.start();
    }

    public void onPacket(Packet packet, Client client) {
        serverChannelManager.onPacket(packet, client);
    }

    public void shutdown() {
        LOGGER.info("shutdown");
        stopped = true;
        serviceLoader.disableAll();
        server.stop();
        System.exit(0);
    }

    public ClientList getClientList() {
        return clientList;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getPort() {
        return port;
    }

    public Config getConfig() {
        return config;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public ServerChannelManager getServerChannelManager() {
        return serverChannelManager;
    }

    public boolean isStopped() {
        return stopped;
    }

    public Server getServer() {
        return server;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}
