package org.by1337.btcp.server.service;


import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.by1337.btcp.server.yaml.YamlContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ServiceLoader {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ServiceLoader.class);
    private final File dir;
    private final Map<String, AbstractService> services = new ConcurrentHashMap<>();
    private final List<ServiceClassLoader> loaders = new CopyOnWriteArrayList<>();
    private final ServiceInitializer serviceInitializer;
    private final DedicatedServer server;

    public ServiceLoader(@NotNull File dir, DedicatedServer server) {
        this.dir = dir;
        this.server = server;
        if (!dir.exists()) {
            dir.mkdirs();
        }
        serviceInitializer = new ServiceInitializer(this);
        serviceInitializer.process();
    }

    public void loadService(File file, ServiceDescriptionFile description) throws IOException, InvalidServiceException {
        if (services.containsKey(description.getName())) {
            var v = services.get(description.getName());
            throw new InvalidServiceException(
                    "Duplicate! " + description.getName() + "-" + description.getVersion() + " & " +
                    v.getName() + "-" + v.getDescription().getVersion()
            );
        }
        File dataFolder = new File(dir + "/" + description.getName());

        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        ServiceClassLoader loader = new ServiceClassLoader(ServiceLoader.class.getClassLoader(), description, dataFolder, file, this, server);
        AbstractService service = loader.getService();
        services.put(service.getName(), service);
        loaders.add(loader);
    }

    public void onLoadPingAll() {
        serviceInitializer.onLoad();
    }

    public void onLoadPing(String name) {
        AbstractService module = getService(name);
        if (module == null) {
            throw new IllegalArgumentException("unknown service: " + name);
        }
        if (module.isEnabled()) return;
        try {
            module.onLoad();
        } catch (Throwable t) {
            LOGGER.error("failed to load service: " + module.getName(), t);
        }
    }

    public void enable(String name) {
        AbstractService module = getService(name);
        if (module == null) {
            throw new IllegalArgumentException("unknown service: " + name);
        }
        if (module.isEnabled()) return;
        module.getLogger().info("enabling...");
        try {
            module.setEnabled(true);
        } catch (Throwable e) {
            LOGGER.error("failed to enable service: " + module.getName(), e);
            disable(name);
        }
    }

    public void disable(String name) {
        AbstractService module = getService(name);
        if (module == null) {
            throw new IllegalArgumentException("unknown service: " + name);
        }
        if (!module.isEnabled()) return;
        module.getLogger().info("disabling...");
        try {
            module.setEnabled(false);
        } catch (Throwable e) {
            LOGGER.error("failed to disable service: " + module.getName(), e);
        }
    }

    public void unloadAll() {
        serviceInitializer.unload();
    }

    public void unload(String name) {
        AbstractService module = getService(name);
        if (module == null) {
            throw new IllegalArgumentException("unknown service: " + name);
        }
        if (module.isEnabled()) {
            disable(name);
        }
        if (module.getClassLoader() instanceof ServiceClassLoader loader) {
            try {
                loader.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close class loader!", e);
            }
            loaders.remove(loader);
        }
        services.remove(name);

    }

    public void enableAll() {
        serviceInitializer.onEnable();
    }

    public void disableAll() {
        serviceInitializer.onDisable();
    }

    private void removeService(String name) {
        services.remove(name);
    }

    @Nullable
    public AbstractService getService(String name) {
        return services.get(name);
    }


    public static YamlContext readFileContentFromJar(String jar) throws IOException {
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry("addon.yml");
            if (entry == null){
                entry = jarFile.getJarEntry("service.yml");
            }
            if (entry != null) {
                try (InputStream inputStream = jarFile.getInputStream(entry);
                     InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                     BufferedReader reader = new BufferedReader(inputStreamReader)) {

                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append(System.lineSeparator());
                    }
                    return new YamlContext(content.toString());
                } catch (YamlContext.YamlParserException e) {
                    throw new IOException(e);
                }
            } else {
                throw new IOException("Jar does not contain service.yml");
            }
        }
    }

    public String getServicesList() {
        StringBuilder sb = new StringBuilder();
        sb.append("&aServices (").append(services.size()).append("):");
        if (services.isEmpty()) return sb.toString();
        sb.append(" ");
        services.values().forEach(module -> {
            if (module.isEnabled()) sb.append("&a");
            else sb.append("&c");
            sb.append(module.getName()).append("&f, ");
        });
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    @Nullable
    public Class<?> getClassByName(String name, boolean resolve) {
        for (ServiceClassLoader loader1 : loaders) {
            try {
                return loader1.loadClass0(name, resolve, false);
            } catch (ClassNotFoundException ignore) {
            }
        }

        return null;
    }

    public Collection<AbstractService> getServices() {
        return services.values();
    }

    public File getDir() {
        return dir;
    }
}
