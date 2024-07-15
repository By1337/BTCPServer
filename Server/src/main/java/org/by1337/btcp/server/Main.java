package org.by1337.btcp.server;

import com.google.common.base.Joiner;
import org.by1337.btcp.server.util.OptionParser;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Objects;

public class Main {
    public static void main(String[] args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        OptionParser parser = new OptionParser(Joiner.on(" ").join(args));

    }
}
