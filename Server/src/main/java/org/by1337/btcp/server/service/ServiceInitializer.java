package org.by1337.btcp.server.service;


import com.google.common.base.Joiner;
import org.by1337.btcp.server.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

class ServiceInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInitializer.class);
    private final ServiceLoader serviceLoader;
    private final List<WeightedItem<Pair<File, ServiceDescriptionFile>>> sorted = new ArrayList<>();

    public ServiceInitializer(ServiceLoader serviceLoader) {
        this.serviceLoader = serviceLoader;
    }

    public void process() {
        List<Pair<File, ServiceDescriptionFile>> toLoad = findServices();

        for (Pair<File, ServiceDescriptionFile> pair : toLoad) {
            sorted.add(new WeightedItem<>(pair));
        }

        Map<String, WeightedItem<Pair<File, ServiceDescriptionFile>>> lookup = new HashMap<>();
        for (var item : sorted) {
            lookup.put(item.val.getRight().getName(), item);
        }
        boolean hasChange;
        int x = 0;
        do {
            x++;
            hasChange = false;
            main:
            for (var item : new ArrayList<>(sorted)) {
                for (String string : item.val.getRight().getDepend()) {
                    var v = lookup.get(string);
                    if (v == null) {
                        LOGGER.error("Missing depend! plugin " + item.val.getRight().getName() + " depends[ " + Joiner.on(", ").join(item.val.getRight().getDepend()) + " ]");
                        sorted.removeIf(i -> i == item);
                        lookup.remove(item.val.getRight().getName());
                    } else if (item.weight <= v.weight) {
                        hasChange = true;
                        item.weight = v.weight + 1;
                        break main;
                    }
                }
                for (String string : item.val.getRight().getSoftDepend()) {
                    var v = lookup.get(string);
                    if (v != null && item.weight <= v.weight) {
                        hasChange = true;
                        item.weight = v.weight + 1;
                        break main;
                    }
                }
            }
            if (x > 2_000) {
                List<ServiceDescriptionFile> error = new ArrayList<>();
                for (var item : new ArrayList<>(sorted)) {
                    if (item.weight > 1_900) {
                        error.add(item.val.getRight());
                        sorted.removeIf(i -> i == item);
                    }
                }
                LOGGER.error("A cyclic relationship has been discovered between [" + Joiner.on(", ").join(
                        error.stream().map(i -> String.format("{%s-%s %s, %s}", i.getName(), i.getVersion(), i.getDepend(), i.getSoftDepend())).toList()
                ));
                for (ServiceDescriptionFile desc : error) {
                    lookup.remove(desc.getName());
                }
                x = 0;
            }
        } while (hasChange);
        sorted.sort(Comparator.comparingInt(o -> o.weight));
        for (WeightedItem<Pair<File, ServiceDescriptionFile>> item : new ArrayList<>(sorted)) {
            try {
                serviceLoader.loadService(item.val.getLeft(), item.val.getRight());
            } catch (IOException | InvalidServiceException e) {
                LOGGER.error("failed to load service " + item.val.getRight().getName(), e);
                sorted.removeIf(i -> i == item);
            }
        }
    }

    public void onLoad() {
        for (WeightedItem<Pair<File, ServiceDescriptionFile>> item : sorted) {
            serviceLoader.onLoadPing(item.val.getRight().getName());
        }
    }

    public void onEnable() {
        for (WeightedItem<Pair<File, ServiceDescriptionFile>> item : sorted) {
            serviceLoader.enable(item.val.getRight().getName());
        }
    }

    public void onDisable() {
        for (int i = sorted.size() - 1; i >= 0; i--) {
            var v = sorted.get(i);
            serviceLoader.disable(v.val.getRight().getName());
        }
    }

    public void unload() {
        for (int i = sorted.size() - 1; i >= 0; i--) {
            var v = sorted.get(i);
            serviceLoader.unload(v.val.getRight().getName());
        }
    }

    public List<Pair<File, ServiceDescriptionFile>> findServices() {
        List<Pair<File, ServiceDescriptionFile>> toLoad = new ArrayList<>();
        if (!serviceLoader.getDir().exists()) return toLoad;
        List<File> files = getServiceFiles();
        toLoad = new ArrayList<>();
        for (File file : files) {
            if (!file.getName().endsWith(".jar") || file.isDirectory()) continue;
            try {
                toLoad.add(new Pair<>(
                        file,
                        new ServiceDescriptionFile(ServiceLoader.readFileContentFromJar(file.getPath()))
                ));
            } catch (IOException e) {
                LOGGER.error("failed to read file " + file.getPath(), e);
            }
        }
        return toLoad;
    }

    private List<File> getServiceFiles() {
        List<File> result = new ArrayList<>();
        File[] files = serviceLoader.getDir().listFiles();
        if (files != null) {
            result.addAll(Arrays.asList(files));
        }
        File old = new File("./addons");
        if (old.exists() && old.isDirectory()) {
            var arr = old.listFiles();
            if (arr != null) {
                result.addAll(Arrays.asList(arr));
            }
        }
        return result;
    }

    private static class WeightedItem<T> {
        private int weight = 0;
        private final T val;

        public WeightedItem(T val) {
            this.val = val;
        }
    }
}
