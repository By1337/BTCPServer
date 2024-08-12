package org.by1337.btcp.server.addon;


import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class AddonClassLoader extends URLClassLoader {
    private final AddonDescriptionFile description;
    private final URL url;
    private final File dataFolder;
    private final File file;
    private final JarFile jar;
    private final JavaAddon addon;
    private final AddonLoader loader;
    private final DedicatedServer server;
    private final Set<String> seenIllegalAccess = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AddonClassLoader(@Nullable ClassLoader parent, AddonDescriptionFile description, File dataFolder, File file, AddonLoader loader, DedicatedServer server) throws IOException, InvalidAddonException {
        super(new URL[]{file.toURI().toURL()}, parent);

        this.description = description;
        this.url = file.toURI().toURL();
        this.dataFolder = dataFolder;
        this.file = file;
        this.jar = new JarFile(file);
        this.loader = loader;
        this.server = server;

        try {
            Class<?> jarClass;
            try {
                jarClass = Class.forName(description.getMain(), true, this);
            } catch (ClassNotFoundException ex) {
                throw new InvalidAddonException("Cannot find main class `" + description.getMain() + "'", ex);
            }

            Class<? extends JavaAddon> pluginClass;

            try {
                pluginClass = jarClass.asSubclass(JavaAddon.class);
            } catch (ClassCastException ex) {
                throw new InvalidAddonException("main class `" + description.getMain() + "' does not extend JavaAddon", ex);
            }

            addon = pluginClass.newInstance();
        } catch (IllegalAccessException ex) {
            throw new InvalidAddonException("No public constructor", ex);
        } catch (InstantiationException ex) {
            throw new InvalidAddonException("Abnormal addon type", ex);
        }
    }

    synchronized void initialize(@NotNull JavaAddon module) {
        if (module.getClass().getClassLoader() != this) {
            throw new IllegalArgumentException("Cannot initialize module outside of this class loader");
        }
        module.init(dataFolder, description.getName(), description, this, file, server);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            jar.close();
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass0(name, resolve, true);
    }

    Class<?> loadClass0(@NotNull String name, boolean resolve, boolean global) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException ex) {
        }

        if (global) {
            Class<?> result = loader.getClassByName(name, resolve);

            if (result != null && result.getClassLoader() instanceof AddonClassLoader addonClassLoader) {
                String addon = addonClassLoader.addon.getName();
                if (!seenIllegalAccess.contains(addon) &&
                    !description.getDepend().contains(addon) ||
                    !description.getSoftDepend().contains(addon)
                ) {
                    seenIllegalAccess.add(addon);
                    this.addon.getLogger().warn("Loaded class {} from {} which is not a depend or softdepend of this addon.", name, addon);
                }
                return result;
            }
        }
        throw new ClassNotFoundException(name);
    }

    public AddonDescriptionFile getDescription() {
        return description;
    }

    public URL getUrl() {
        return url;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public File getFile() {
        return file;
    }

    public JarFile getJar() {
        return jar;
    }

    public JavaAddon getAddon() {
        return addon;
    }


}
