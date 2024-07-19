package org.by1337.btcp.server.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.by1337.btcp.server.dedicated.DedicatedServer;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.*;

public class StopCommand implements Command {
    @Override
    public LiteralArgumentBuilder<DedicatedServer> create() {
        LiteralArgumentBuilder<DedicatedServer> cmd = literal("stop");
        cmd.executes(server -> {
            server.getSource().shutdown();
            return 1;
        });
        return cmd;
    }
}
