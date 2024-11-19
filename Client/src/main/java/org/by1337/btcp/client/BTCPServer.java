package org.by1337.btcp.client;

import org.by1337.btcp.client.network.Connection;
import org.by1337.btcp.client.network.channel.ClientChannelManager;
@Deprecated
public class BTCPServer {

    public static Connection getConnection() {
        return BTCPClient.getConnection();
    }

    public static ClientChannelManager getClientChannelManager() {
        return BTCPClient.getClientChannelManager();
    }
}
