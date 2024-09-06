package org.by1337.btcp.server.dedicated.client;

import org.by1337.btcp.server.event.ClientConnectedEvent;
import org.by1337.btcp.server.event.ClientDisconnectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientList {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientList.class);
    private final AtomicInteger idCounter = new AtomicInteger();
    private final Map<String, Client> clientMap = new HashMap<>();

    public boolean hasClient(String client) {
        synchronized (clientMap) {
            return clientMap.containsKey(client);
        }
    }

    public void newClient(Client client) {
        synchronized (clientMap) {
            if (clientMap.containsKey(client.getId())) {
                client.disconnect(String.format("The client named %s is already connected!", client.getId()));
                return;
            }
            clientMap.put(client.getId(), client);
        }
        LOGGER.info("new connection [{}:{}]", client.getId(), client.getAddress());
        client.getServer().getEventManager().callEvent(new ClientConnectedEvent(client));
    }

    public void disconnect(Client client, String reason) {
        synchronized (clientMap) {
            clientMap.remove(client.getId());
        }
        LOGGER.info("client disconnected [{}:{}] reason {}", client.getId(), client.getAddress(), reason);
        client.getServer().getEventManager().callEvent(new ClientDisconnectEvent(client, reason));
    }

    public String nextId() {
        return String.format("Client#%d", idCounter.getAndIncrement());
    }

    public Collection<Client> getClients() {
        synchronized (clientMap) {
            return Collections.unmodifiableCollection(clientMap.values());
        }
    }
}
