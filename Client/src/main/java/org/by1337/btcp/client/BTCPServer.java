package org.by1337.btcp.client;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.chat.util.Message;
import org.by1337.blib.configuration.YamlConfig;
import org.by1337.btcp.client.network.Connection;
import org.by1337.btcp.client.network.ConnectionStatusListener;
import org.by1337.btcp.client.network.channel.ClientChannelManager;
import org.by1337.btcp.client.network.channel.impl.MainClientChannel;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.packet.impl.EncryptedPacket;
import org.by1337.btcp.common.packet.impl.channel.ChannelStatusPacket;
import org.by1337.btcp.common.util.id.SpacedName;

import java.io.File;
import java.util.logging.Level;

public class BTCPServer extends JavaPlugin implements ConnectionStatusListener {
    private Message message;
    private static BTCPServer instance;
    private Connection connection;
    private ClientChannelManager clientChannelManager;

    @Override
    public void onLoad() {
        instance = this;
        message = new Message(getLogger());
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
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "failed to enable", e);
            Bukkit.getServer().shutdown();
            if (connection != null) {
                connection.shutdown();
            }
            return;
        }
    }

    public void onPacket(Packet packet) {
        clientChannelManager.onPacket(packet);
    }

    public void onDisconnect() {
        Bukkit.getServer().shutdown();
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        if (connection != null && connection.isRunning()) {
            connection.shutdown();
        }
    }

    public static Connection getConnection() {
        return instance.connection;
    }

    public static ClientChannelManager getClientChannelManager() {
        return instance.clientChannelManager;
    }
}
