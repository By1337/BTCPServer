package org.by1337.btcp.server.dedicated;

import org.by1337.btcp.server.yaml.YamlContext;

public class Config {
    private final boolean allowAnonymousClients;
    private final String secretKey;
    private final int threshold;
    private final int compressionLvl;

    public Config(YamlContext context) {
        allowAnonymousClients = context.get("server.clients.allow-anonymous-clients").getAsBoolean(false);
        secretKey = context.get("server.clients.secret-key").getAsString();
        threshold = context.get("server.network.threshold").getAsInteger();
        compressionLvl = context.get("server.network.compression-lvl").getAsInteger();
    }

    public boolean isAllowAnonymousClients() {
        return allowAnonymousClients;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getCompressionLvl() {
        return compressionLvl;
    }
}
