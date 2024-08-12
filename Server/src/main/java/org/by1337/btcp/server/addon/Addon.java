package org.by1337.btcp.server.addon;

import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;


public interface Addon {
    @NotNull
    File getDataFolder();

    @Nullable
    InputStream getResource(@NotNull String file);

    void saveResource(@NotNull String file, boolean replace);

    @NotNull
    Logger getLogger();

    @NotNull
    String getName();

    boolean isEnabled();

    void onLoad();

    void onEnable();

    void onDisable();

    @NotNull
    AddonDescriptionFile getDescription();

    DedicatedServer getServer();
}
