package org.by1337.btcp.server.service;


import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

public class ServiceClassLoader extends URLClassLoader {
    private final ServiceDescriptionFile description;
    private final URL url;
    private final File dataFolder;
    private final File file;
    private final JarFile jar;
    private final AbstractService service;
    private final ServiceLoader loader;
    private final DedicatedServer server;
    private final Set<String> seenIllegalAccess = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ServiceClassLoader(@Nullable ClassLoader parent, ServiceDescriptionFile description, File dataFolder, File file, ServiceLoader loader, DedicatedServer server) throws IOException, InvalidServiceException {
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
                throw new InvalidServiceException("Cannot find main class `" + description.getMain() + "'", ex);
            }

            Class<? extends AbstractService> pluginClass;

            try {
                pluginClass = jarClass.asSubclass(AbstractService.class);
            } catch (ClassCastException ex) {
                throw new InvalidServiceException("main class `" + description.getMain() + "' does not extend AbstractService", ex);
            }

            service = pluginClass.newInstance();
        } catch (IllegalAccessException ex) {
            throw new InvalidServiceException("No public constructor", ex);
        } catch (InstantiationException ex) {
            throw new InvalidServiceException("Abnormal service type", ex);
        }
    }

    synchronized void initialize(@NotNull AbstractService module) {
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
    public URL getResource(String name) {
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
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

            if (result != null && result.getClassLoader() instanceof ServiceClassLoader serviceClassLoader) {
                String service = serviceClassLoader.service.getName();
                if (!seenIllegalAccess.contains(service) &&
                    !description.getDepend().contains(service) ||
                    !description.getSoftDepend().contains(service)
                ) {
                    seenIllegalAccess.add(service);
                    this.service.getLogger().warn("Loaded class {} from {} which is not a depend or softdepend of this service.", name, service);
                }
                return result;
            }
        }
        throw new ClassNotFoundException(name);
    }

    public ServiceDescriptionFile getDescription() {
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

    public AbstractService getService() {
        return service;
    }


}
