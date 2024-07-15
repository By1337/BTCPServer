package org.by1337.btcp.server.yaml;

import com.google.common.base.Joiner;
import org.by1337.btcp.server.util.validate.Validate;
import org.by1337.btcp.server.yaml.adapter.AdapterRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.*;

public class YamlContext {
    private LinkedHashMap<String, Object> raw;

    public YamlContext(LinkedHashMap<String, Object> raw) {
        this.raw = raw;
    }

    public YamlContext() {
        raw = new LinkedHashMap<>();
    }

    public YamlContext(String str) throws YamlParserException {
        try {
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(options);
            raw = yaml.load(str);
        } catch (Exception e) {
            throw new YamlParserException(e);
        }
    }

    public YamlContext(File file) throws YamlParserException, IOException {
        this(Files.readString(file.toPath()));
    }

    public void set(@NotNull String path, @Nullable Object obj) {
        Validate.notNull(path, "path is null!");
        Validate.test(path, String::isBlank, () -> new IllegalStateException("path is empty"));

        String[] pathParts = path.split("\\.");
        Map<String, Object> currentMap = raw;

        for (int i = 0; i < pathParts.length; i++) {
            String key = pathParts[i];

            if (i == pathParts.length - 1) {
                currentMap.put(key, processObject(obj));
            } else {
                Object value = currentMap.get(key);
                if (value instanceof Map) {
                    currentMap = (Map<String, Object>) value;
                } else if (value == null) {
                    Map<String, Object> newMap = new HashMap<>();
                    currentMap.put(key, newMap);
                    currentMap = newMap;
                } else {
                    throw new ClassCastException(Joiner.on(".").join(Arrays.copyOf(pathParts, i)) + " is primitive!");
                }
            }
        }
    }

    private Object processObject(Object obj) {
        if (obj instanceof Collection<?> collection) {
            return collection.stream().map(AdapterRegistry::serialize).toList();
        } else if (obj instanceof Map<?, ?> map) {
            Map<Object, Object> processedMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                processedMap.put(AdapterRegistry.serialize(entry.getKey()), AdapterRegistry.serialize(entry.getValue()));
            }
            return processedMap;
        } else {
            return AdapterRegistry.serialize(obj);
        }
    }

    @NotNull
    public YamlValue get(@NotNull String path, Object def) {
        var obj = get(path);
        return obj.getValue() == null ? new YamlValue(def) : obj;
    }

    @NotNull
    public YamlValue get(@NotNull String path) {
        Validate.notNull(path, "path is null!");
        Validate.test(path, String::isBlank, () -> new IllegalStateException("path is empty"));
        String[] path0 = path.split("\\.");

        Object last = null;
        for (String s : path0) {
            if (last == null) {
                Object o = raw.get(s);
                if (o == null) return YamlValue.EMPTY;
                last = o;
            } else if (last instanceof Map<?, ?> sub) {
                Object o = sub.get(s);
                if (o == null) return YamlValue.EMPTY;
                last = o;
            } else {
                throw new ClassCastException(last.getClass().getName() + " to Map<String, Object>");
            }
        }
        return new YamlValue(last);
    }

    public String saveToString() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        StringWriter writer = new StringWriter();
        yaml.dump(raw, writer);
        return writer.toString();
    }

    public void saveToFile(String fileName) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(fileName)) {
            yaml.dump(raw, writer);
        }
    }

    public LinkedHashMap<String, Object> getRaw() {
        return raw;
    }

    public static class YamlParserException extends Exception {
        public YamlParserException() {
        }

        public YamlParserException(String message) {
            super(message);
        }

        public YamlParserException(String message, Throwable cause) {
            super(message, cause);
        }

        public YamlParserException(Throwable cause) {
            super(cause);
        }

        public YamlParserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
