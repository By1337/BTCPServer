package org.by1337.btcp.client;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.configuration.YamlConfig;
import org.by1337.btcp.client.network.Connection;
import org.by1337.btcp.client.network.ConnectionStatusListener;
import org.by1337.btcp.client.network.channel.ClientChannelManager;
import org.by1337.btcp.client.network.channel.forward.ForwardChannelManager;
import org.by1337.btcp.client.network.channel.impl.MainClientChannel;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.EncryptedPacket;
import org.by1337.btcp.common.packet.impl.channel.ChannelStatusPacket;
import org.by1337.btcp.common.util.id.SpacedName;

import java.io.File;
import java.util.logging.Level;

public class BTCPClient extends JavaPlugin implements ConnectionStatusListener {
    private static BTCPClient instance;
    private Connection connection;
    private ClientChannelManager clientChannelManager;
    private String onDisconnect;
    private ForwardChannelManager forwardChannelManager;

    @Override
    public void onLoad() {
        instance = this;
        File cfg = new File(getDataFolder() + "/config.yml");
        if (!cfg.exists()) {
            saveResource("config.yml", true);
        }
        try {
            YamlConfig config = new YamlConfig(cfg);
            String id = config.getAsString("current-server-id");
            if (id.equals("auto_gen")) {
                id = null;
            }
            onDisconnect = config.get("on-disconnect").getAsString("stop-server");
            String secretKey = config.get("secret-key").getAsString();
            EncryptedPacket.setSecretKey(secretKey);
            connection = new Connection(
                    getSLF4JLogger(),
                    config.get("ip").getAsString(),
                    config.get("port").getAsInteger(),
                    id,
                    config.get("password").getAsString(),
                    secretKey,
                    this
            );

            connection.start(config.get("debug").getAsBoolean(false));
            synchronized (connection) {
                connection.wait(10_000);
            }
            if (!connection.isAuthorized()) {
                throw new IllegalStateException("Failed to login!");
            }
            clientChannelManager = new ClientChannelManager(connection);
            MainClientChannel mainClientChannel = new MainClientChannel(clientChannelManager, new SpacedName("native", "main"));
            mainClientChannel.register().sync(5_000);
            if (mainClientChannel.getStatus() != ChannelStatusPacket.ChannelStatus.OPENED) {
                mainClientChannel.unregister();
                throw new IllegalStateException("Failed to open channel native:main");
            }
            forwardChannelManager = new ForwardChannelManager(clientChannelManager, this);

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "failed to enable", e);
            Bukkit.getServer().shutdown();
            if (connection != null) {
                connection.shutdown();
            }
            return;
        }
    }

    @Override
    public void onEnable() {
        forwardChannelManager.onEnable();
    }

    public void onPacket(Packet packet) {
        clientChannelManager.onPacket(packet);
    }

    public void onDisconnect() {
        connection = null;
        if ("disable-all-depend-plugins".equalsIgnoreCase(onDisconnect)) {
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (
                        plugin.getDescription().getDepend().contains(getDescription().getName()) //||
                    //  plugin.getDescription().getSoftDepend().contains(getDescription().getName()) ||
                    //    plugin.getDescription().getLoadBefore().contains(getDescription().getName())
                ) {
                    try {
                        Bukkit.getPluginManager().disablePlugin(plugin);
                    } catch (Throwable t) {
                        plugin.getSLF4JLogger().error("Failed to disable plugin {}", plugin.getName(), t);
                    }
                }
            }
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            Bukkit.getServer().shutdown();
        }
        forwardChannelManager.unregister();
    }


    @Override
    public void onDisable() {
        if (connection != null && connection.isRunning()) {
            connection.shutdown();
        }
    }

    public static ForwardChannelManager getForwardChannelManager() {
        return instance.forwardChannelManager;
    }

    public static boolean isAvailable(){
        return instance.connection != null && instance.connection.isRunning();
    }

    public static Connection getConnection() {
        return instance.connection;
    }

    public static ClientChannelManager getClientChannelManager() {
        return instance.clientChannelManager;
    }
}
