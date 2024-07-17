package org.by1337.btcp.client;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.chat.util.Message;
import org.by1337.blib.configuration.YamlConfig;
import org.by1337.btcp.client.tcp.Connection;

import java.io.File;
import java.util.logging.Level;

public class BTCPServer extends JavaPlugin {
    private Message message;
    private Connection connection;
    @Override
    public void onLoad() {
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
            Connection connection = new Connection(
                    getSLF4JLogger(),
                    config.get("ip").getAsString(),
                    config.get("port").getAsInteger(),
                    id,
                    config.get("password").getAsString(),
                    config.get("secret-key").getAsString()
            );
            connection.start(true);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "failed to enable", e);
            Bukkit.getServer().shutdown();
            return;
        }
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        if (connection != null){
            connection.shutdown();
        }
    }


}
