package org.by1337.btcp.server.addon;


import com.google.common.base.Joiner;
import org.by1337.btcp.server.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

class AddonInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddonInitializer.class);
    private final AddonLoader addonLoader;
    private List<WeightedItem<Pair<File, AddonDescriptionFile>>> sorted = new ArrayList<>();

    public AddonInitializer(AddonLoader addonLoader) {
        this.addonLoader = addonLoader;
    }

    public void process() {
        List<Pair<File, AddonDescriptionFile>> toLoad = findAddons();

        for (Pair<File, AddonDescriptionFile> pair : toLoad) {
            sorted.add(new WeightedItem<>(pair));
        }

        Map<String, WeightedItem<Pair<File, AddonDescriptionFile>>> lookup = new HashMap<>();
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
                List<AddonDescriptionFile> error = new ArrayList<>();
                for (var item : new ArrayList<>(sorted)) {
                    if (item.weight > 1_900) {
                        error.add(item.val.getRight());
                        sorted.removeIf(i -> i == item);
                    }
                }
                LOGGER.error("A cyclic relationship has been discovered between [" + Joiner.on(", ").join(
                        error.stream().map(i -> String.format("{%s-%s %s, %s}", i.getName(), i.getVersion(), i.getDepend(), i.getSoftDepend())).toList()
                ));
                for (AddonDescriptionFile desc : error) {
                    lookup.remove(desc.getName());
                }
                x = 0;
            }
        } while (hasChange);
        sorted.sort(Comparator.comparingInt(o -> o.weight));
        for (WeightedItem<Pair<File, AddonDescriptionFile>> item : new ArrayList<>(sorted)) {
            try {
                addonLoader.loadAddon(item.val.getLeft(), item.val.getRight());
            } catch (IOException | InvalidAddonException e) {
                LOGGER.error("failed to load addon " + item.val.getRight().getName(), e);
                sorted.removeIf(i -> i == item);
            }
        }
    }

    public void onLoad() {
        for (WeightedItem<Pair<File, AddonDescriptionFile>> item : sorted) {
            addonLoader.onLoadPing(item.val.getRight().getName());
        }
    }

    public void onEnable() {
        for (WeightedItem<Pair<File, AddonDescriptionFile>> item : sorted) {
            addonLoader.enable(item.val.getRight().getName());
        }
    }

    public void onDisable() {
        for (int i = sorted.size() - 1; i >= 0; i--) {
            var v = sorted.get(i);
            addonLoader.disable(v.val.getRight().getName());
        }
    }
    public void unload() {
        for (int i = sorted.size() - 1; i >= 0; i--) {
            var v = sorted.get(i);
            addonLoader.unload(v.val.getRight().getName());
        }
    }

    public List<Pair<File, AddonDescriptionFile>> findAddons() {
        List<Pair<File, AddonDescriptionFile>> toLoad = new ArrayList<>();
        if (!addonLoader.getDir().exists()) return toLoad;
        File[] files = addonLoader.getDir().listFiles();
        if (files == null) return toLoad;
        toLoad = new ArrayList<>();
        for (File file : files) {
            if (!file.getName().endsWith(".jar") || file.isDirectory()) continue;
            try {
                toLoad.add(new Pair<>(
                        file,
                        new AddonDescriptionFile(AddonLoader.readFileContentFromJar(file.getPath()))
                ));
            } catch (IOException e) {
                LOGGER.error("failed to read file " + file.getPath(), e);
            }
        }
        return toLoad;
    }

    private static class WeightedItem<T> {
        private int weight = 0;
        private final T val;

        public WeightedItem(T val) {
            this.val = val;
        }
    }
}
