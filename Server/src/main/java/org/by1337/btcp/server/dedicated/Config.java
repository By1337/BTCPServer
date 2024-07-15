package org.by1337.btcp.server.dedicated;

import org.by1337.btcp.server.yaml.YamlContext;

public class Config {
    private final boolean allowAnonymousClients;

    public Config(YamlContext context) {
        allowAnonymousClients = context.get("server.clients.allow-anonymous-clients").getAsBoolean(false);
    }

    public boolean isAllowAnonymousClients() {
        return allowAnonymousClients;
    }
}
