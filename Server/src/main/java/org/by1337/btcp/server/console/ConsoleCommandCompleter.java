package org.by1337.btcp.server.console;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import org.by1337.btcp.server.commands.CommandManager;
import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import javax.annotation.processing.Completion;
import java.util.ArrayList;
import java.util.List;

public class ConsoleCommandCompleter implements Completer {
    private final CommandManager commandManager;
    private final DedicatedServer server;

    public ConsoleCommandCompleter(CommandManager commandManager, DedicatedServer server) {
        this.commandManager = commandManager;
        this.server = server;
    }

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
        final ParseResults<DedicatedServer> results = commandManager.getRootCommand().parse(prepareStringReader(parsedLine.line()), server);
        var result =  commandManager.getRootCommand().getCompletionSuggestions(results, parsedLine.cursor()).join().getList();
        for (Suggestion suggestion : result) {
            list.add(new Candidate(suggestion.getText()));
        }
    }


    static @NonNull StringReader prepareStringReader(final @NonNull String line) {
        final StringReader stringReader = new StringReader(line);
        if (stringReader.canRead() && stringReader.peek() == '/') {
            stringReader.skip();
        }
        return stringReader;
    }
}
