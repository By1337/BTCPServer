package org.by1337.btcp.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import org.by1337.btcp.server.dedicated.DedicatedServer;

public class CommandManager {
    private final CommandDispatcher<DedicatedServer> rootCommand = new CommandDispatcher<>();
    private final DedicatedServer dedicatedServer;

    public CommandManager(DedicatedServer dedicatedServer) {
        this.dedicatedServer = dedicatedServer;
        rootCommand.register(new StopCommand().create());
    }

    public CommandDispatcher<DedicatedServer> getRootCommand() {
        return rootCommand;
    }
}
