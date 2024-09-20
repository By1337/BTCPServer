package org.by1337.btcp.server;

import com.google.common.base.Joiner;
import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.by1337.btcp.server.util.OptionParser;
import org.by1337.btcp.server.yaml.YamlContext;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws YamlContext.YamlParserException, IOException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        OptionParser parser = new OptionParser(Joiner.on(" ").join(args));

        new DedicatedServer(parser);
    }
}
