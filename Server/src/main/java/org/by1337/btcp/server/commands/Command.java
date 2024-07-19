package org.by1337.btcp.server.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.by1337.btcp.server.dedicated.DedicatedServer;

public interface Command {
    LiteralArgumentBuilder<DedicatedServer> create();
}
