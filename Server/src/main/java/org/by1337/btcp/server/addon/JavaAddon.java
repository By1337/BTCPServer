package org.by1337.btcp.server.addon;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

public abstract class JavaAddon implements Addon {
    private Logger logger;
    private boolean isEnabled = false;
    private File dataFolder;
    private String name;
    private AddonDescriptionFile descriptionFile;
    private ClassLoader classLoader;
    private File file;

    public JavaAddon() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (classLoader instanceof AddonClassLoader loader) {
            loader.initialize(this);
        } else {
            throw new IllegalArgumentException("JavaAddon requires " + AddonClassLoader.class.getName());
        }
    }

    final void init(File dataFolder, String name, AddonDescriptionFile descriptionFile, ClassLoader classLoader, File file) {
        this.dataFolder = dataFolder;
        this.name = name;
        this.descriptionFile = descriptionFile;
        this.classLoader = classLoader;
        this.file = file;
        logger = LoggerFactory.getLogger(name);
    }

    @NotNull
    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public AddonDescriptionFile getDescription() {
        return descriptionFile;
    }
    protected final void setEnabled(boolean enabled) {
        if (isEnabled != enabled) {
            isEnabled = enabled;
            if (isEnabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    @Override
    public @Nullable InputStream getResource(@NotNull String filename) {
        try {
            URL url = getClassLoader().getResource(filename);
            if (url == null) {
                return null;
            }
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public void saveResource(@NotNull String resourcePath, boolean replace) {
        if (resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + file);
        }

        File outFile = new File(dataFolder, resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(dataFolder, resourcePath.substring(0, Math.max(lastIndex, 0)));
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
        } catch (IOException ex) {
            logger.error("Could not saveAndPrint " + outFile.getName() + " to " + outFile, ex);
        }
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @NotNull
    public final ClassLoader getClassLoader() {
        return classLoader;
    }
}
