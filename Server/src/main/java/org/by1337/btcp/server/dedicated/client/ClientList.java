package org.by1337.btcp.server.dedicated.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientList {
    private final AtomicInteger idCounter = new AtomicInteger();
    private final Map<String, Client> clientMap = new HashMap<>();

    public boolean hasClient(String client) {
        synchronized (clientMap){
            return clientMap.containsKey(client);
        }
    }

    public void newClient(Client client) {

    }

    public void disconnect(Client client, String reason) {

    }

    public String nextId() {
        return String.format("Client#%d", idCounter.getAndIncrement());
    }
}
