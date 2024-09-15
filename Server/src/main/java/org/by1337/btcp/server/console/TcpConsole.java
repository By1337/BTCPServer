package org.by1337.btcp.server.console;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.by1337.btcp.server.commands.CommandManager;
import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TcpConsole {
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpConsole.class);
    private final DedicatedServer server;
    private final CommandManager commandManager;

    public TcpConsole(DedicatedServer server, CommandManager commandManager) {
        this.server = server;
        this.commandManager = commandManager;
    }

    public void start() {
        try {
            Terminal terminal = TerminalBuilder.builder().encoding("UTF-8").dumb(true).build();
            this.readCommands(terminal);
        } catch (IOException var2) {
            LOGGER.error("Error while reading commands", var2);
        }
    }

    private void readCommands(Terminal terminal) {
        LineReader reader = this.buildReader(LineReaderBuilder.builder().terminal(terminal));
        try {
            while (!server.isStopped()) {
                String line;
                try {
                    line = reader.readLine("> ");
                } catch (EndOfFileException var9) {
                    continue;
                }

                if (line == null) {
                    break;
                }
                if (line.isBlank()) continue;

                this.processInput(line.trim());
            }
        } catch (UserInterruptException var10) {
            throw new RuntimeException(var10);
        }
    }

    protected void processInput(String input) {
        try {
            final ParseResults<DedicatedServer> parse = commandManager.getRootCommand().parse(input, server);
            commandManager.getRootCommand().execute(parse);
        } catch (CommandSyntaxException e) {
            LOGGER.error("An error occurred while executing the command", e);
        }
    }

    protected LineReader buildReader(LineReaderBuilder builder) {
        builder
                .appName("TcpServer")
                .variable(LineReader.HISTORY_FILE, java.nio.file.Paths.get(".console_history"))
                .completer(new ConsoleCommandCompleter(commandManager, server))
                .option(LineReader.Option.COMPLETE_IN_WORD, true);
        return builder.build();
    }
}
