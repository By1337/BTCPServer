package org.by1337.btcp.server.event;

import org.by1337.btcp.common.event.Event;
import org.by1337.btcp.server.dedicated.client.Client;

public class ClientConnectedEvent implements Event {
    private final Client client;

    public ClientConnectedEvent(Client client) {
        this.client = client;
    }

    public Client getClient() {
        return client;
    }
}
