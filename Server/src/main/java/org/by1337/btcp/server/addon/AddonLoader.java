package org.by1337.btcp.server.addon;


import org.by1337.btcp.server.dedicated.DedicatedServer;
import org.by1337.btcp.server.yaml.YamlContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddonLoader {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AddonLoader.class);
    private final File dir;
    private final Map<String, JavaAddon> addons = new ConcurrentHashMap<>();
    private final List<AddonClassLoader> loaders = new CopyOnWriteArrayList<>();
    //private final List<URLClassLoader> libs = new CopyOnWriteArrayList<>();
    private final AddonInitializer addonInitializer;
    private final DedicatedServer server;

    public AddonLoader(@NotNull File dir, DedicatedServer server) {
        this.dir = dir;
        this.server = server;
        if (!dir.exists()){
            dir.mkdirs();
        }
        addonInitializer = new AddonInitializer(this);
        addonInitializer.process();
    }

    public void loadAddon(File file, AddonDescriptionFile description) throws IOException, InvalidAddonException {
        if (addons.containsKey(description.getName())) {
            var v = addons.get(description.getName());
            throw new InvalidAddonException(
                    "Duplicate! " + description.getName() + "-" + description.getVersion() + " & " +
                    v.getName() + "-" + v.getDescription().getVersion()
            );
        }
        File dataFolder = new File(dir + "/" + description.getName());

        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        AddonClassLoader loader = new AddonClassLoader(AddonLoader.class.getClassLoader(), description, dataFolder, file, this, server);
        JavaAddon addon = loader.getAddon();
        addons.put(addon.getName(), addon);
        loaders.add(loader);
    }

    public void onLoadPingAll() {
        addonInitializer.onLoad();
    }

    public void onLoadPing(String name) {
        JavaAddon module = getAddon(name);
        if (module == null) {
            throw new IllegalArgumentException("unknown addon: " + name);
        }
        if (module.isEnabled()) return;
        try {
            module.onLoad();
        } catch (Throwable t) {
            LOGGER.error("failed to load addon: " + module.getName(), t);
        }
    }

    public void enable(String name) {
        JavaAddon module = getAddon(name);
        if (module == null) {
            throw new IllegalArgumentException("unknown addon: " + name);
        }
        if (module.isEnabled()) return;
        module.getLogger().info("enabling...");
        try {
            module.setEnabled(true);
        } catch (Throwable e) {
            LOGGER.error("failed to enable addon: " + module.getName(), e);
            disable(name);
        }
    }

    public void disable(String name) {
        JavaAddon module = getAddon(name);
        if (module == null) {
            throw new IllegalArgumentException("unknown addon: " + name);
        }
        if (!module.isEnabled()) return;
        module.getLogger().info("disabling...");
        try {
            module.setEnabled(false);
        } catch (Throwable e) {
            LOGGER.error("failed to disable addon: " + module.getName(), e);
        }
    }

    public void unloadAll() {
        addonInitializer.unload();
    }

    public void unload(String name) {
        JavaAddon module = getAddon(name);
        if (module == null) {
            throw new IllegalArgumentException("unknown addon: " + name);
        }
        if (module.isEnabled()) {
            disable(name);
        }
        if (module.getClassLoader() instanceof AddonClassLoader loader) {
            try {
                loader.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close class loader!", e);
            }
            loaders.remove(loader);
        }
        addons.remove(name);

    }

    public void enableAll() {
        addonInitializer.onEnable();
    }

    public void disableAll() {
        addonInitializer.onDisable();
    }

    private void removeAddon(String name) {
        addons.remove(name);
    }

    @Nullable
    public JavaAddon getAddon(String name) {
        return addons.get(name);
    }


    public static YamlContext readFileContentFromJar(String jar) throws IOException {
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry("addon.yml");
            if (entry != null) {try (InputStream inputStream = jarFile.getInputStream(entry);
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
                throw new IOException("Jar does not contain addon.yml");
            }
        }
    }

    public String getAddonList() {
        StringBuilder sb = new StringBuilder();
        sb.append("&aAddons (").append(addons.size()).append("):");
        if (addons.isEmpty()) return sb.toString();
        sb.append(" ");
        addons.values().forEach(module -> {
            if (module.isEnabled()) sb.append("&a");
            else sb.append("&c");
            sb.append(module.getName()).append("&f, ");
        });
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    @Nullable
    public Class<?> getClassByName(String name, boolean resolve) {
        for (AddonClassLoader loader1 : loaders) {
            try {
                return loader1.loadClass0(name, resolve, false);
            } catch (ClassNotFoundException ignore) {
            }
        }

        return null;
       // return findClassInLibs(name);
    }

//    @Nullable
//    public Class<?> findClassInLibs(String name) {
//        for (URLClassLoader lib : libs) {
//            try {
//                return lib.loadClass(name);
//            } catch (ClassNotFoundException ignore) {
//            }
//        }
//        return null;
//    }


//    public void loadLib(File jar) throws MalformedURLException {
//        logger.info("load lib " + jar.getName() + "...");
//        libs.add(new URLClassLoader(new URL[]{jar.toURI().toURL()}, AddonLoader.class.getClassLoader()));
//    }
//
//    public boolean isLibLoader(URLClassLoader urlClassLoader) {
//        return libs.contains(urlClassLoader);
//    }

//    public void closeAndClearLibs() {
//        for (URLClassLoader lib : libs) {
//            try {
//                lib.close();
//            } catch (IOException e) {
//                logger.log(Level.SEVERE, "failed to unload lib!", e);
//            }
//        }
//        libs.clear();
//    }

    public Collection<JavaAddon> getAddons() {
        return addons.values();
    }

    public File getDir() {
        return dir;
    }
}
