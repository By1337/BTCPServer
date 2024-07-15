package org.by1337.btcp.server.dedicated;

import org.by1337.btcp.server.yaml.YamlContext;

public class Config {
    private final boolean allowAnonymousClients;
    private final String secretKey;

    public Config(YamlContext context) {
        allowAnonymousClients = context.get("server.clients.allow-anonymous-clients").getAsBoolean(false);
        secretKey = context.get("server.clients.secret-key").getAsString();
    }

    public boolean isAllowAnonymousClients() {
        return allowAnonymousClients;
    }

    public String getSecretKey() {
        return secretKey;
    }
}
