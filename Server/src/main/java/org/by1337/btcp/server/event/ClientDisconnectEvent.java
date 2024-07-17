package org.by1337.btcp.server.event;

import org.by1337.btcp.common.event.Event;
import org.by1337.btcp.server.dedicated.client.Client;

public class ClientDisconnectEvent implements Event {
    private final Client client;
    private final String reason;
    public ClientDisconnectEvent(Client client, String reason) {
        this.client = client;
        this.reason = reason;
    }

    public Client getClient() {
        return client;
    }

    public String getReason() {
        return reason;
    }
}
