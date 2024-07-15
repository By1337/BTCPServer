package org.by1337.btcp.server.dedicated;

import org.by1337.btcp.server.dedicated.client.ClientList;
import org.by1337.btcp.server.util.OptionParser;
import org.by1337.btcp.server.util.resource.ResourceUtil;
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

    public DedicatedServer(OptionParser parser) throws YamlContext.YamlParserException, IOException {
        YamlContext context = new YamlContext(ResourceUtil.saveResource("config.yml", false, new File("./")));
        debug = Boolean.parseBoolean(parser.getOrDefault("debug", context.get("server.debug").getAsString()));
        password = parser.getOrDefault("password", context.get("server.password").getAsString());
        port = Integer.parseInt(parser.getOrDefault("port", context.get("server.port").getAsString()));
        System.setProperty("io.netty.eventLoopThreads", context.get("server.netty-threads").getAsString("6"));
        config = new Config(context);
        clientList = new ClientList();
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
}
