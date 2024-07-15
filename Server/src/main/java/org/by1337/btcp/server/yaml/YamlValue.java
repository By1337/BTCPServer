package org.by1337.btcp.server.yaml;

import org.by1337.btcp.server.util.Pair;
import org.by1337.btcp.server.yaml.adapter.AdapterRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YamlValue {
    public static final YamlValue EMPTY = new YamlValue(null);
    private final @Nullable Object value;

    public YamlValue(@Nullable Object value) {
        this.value = value;
    }


    public <T> T getAs(Class<T> type) {
        return AdapterRegistry.getAs(value, type);
    }

    public <T> T getAs(Class<T> type, T def) {
        if (value == null) return def;
        return getAs(type);
    }


    public <T> List<T> getAsList(Function<YamlValue, T> function, List<T> def) {
        if (value == null) return def;
        return getAsList(function);
    }

    public <T> List<T> getAsList(Class<T> type) {
        return getAsList(v -> v.getAs(type));
    }

    public <T> List<T> getAsList(Function<YamlValue, T> function) {
        return stream()
                .map(function)
                .collect(Collectors.toList());
    }

    public Stream<YamlValue> stream() {
        return ((List<?>) value).stream().map(YamlValue::new);
    }

    @SuppressWarnings("unchecked")
    public Stream<Pair<YamlValue, YamlValue>> mapStream() {
        Map<String, ?> map0 = (Map<String, ?>) value;
        return map0.entrySet().stream().map(e -> Pair.of(new YamlValue(e.getKey()), new YamlValue(e.getValue())));
    }

    public <V> Map<String, V> getAsMap(Class<V> valueType) {
        return getAsMap(String.class, valueType);
    }

    public <V> Map<String, V> getAsMap(Class<V> valueType, Map<String, V> def) {
        return getAsMap(String.class, valueType, def);
    }

    public <K, V> Map<K, V> getAsMap(Class<K> keyType, Class<V> valueType, Map<K, V> def) {
        if (value == null) return def;
        return getAsMap(keyType, valueType);
    }

    public <K, V> Map<K, V> getAsMap(Class<K> keyType, Class<V> valueType) {
        return getAsMap(k -> k.getAs(keyType), v -> v.getAs(valueType));
    }

    public <K, V> Map<K, V> getAsMap(Function<YamlValue, K> keyMapper, Function<YamlValue, V> valueMapper, Map<K, V> def) {
        if (value == null) return def;
        return getAsMap(keyMapper, valueMapper);
    }

    public <V> Map<String, V> getAsMap(Function<YamlValue, V> valueMapper, Map<String, V> def) {
        if (value == null) return def;
        return getAsMap(valueMapper);
    }

    public <V> Map<String, V> getAsMap(Function<YamlValue, V> valueMapper) {
        return getAsMap(YamlValue::getAsString, valueMapper);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getAsMap(Function<YamlValue, K> keyMapper, Function<YamlValue, V> valueMapper) {
        Map<String, ?> map0 = (Map<String, ?>) value;
        Map<K, V> map = new HashMap<>();
        for (Map.Entry<String, ?> entry : map0.entrySet()) {
            map.put(keyMapper.apply(new YamlValue(entry.getKey())), valueMapper.apply(new YamlValue(entry.getValue())));
        }
        return map;
    }

    public Double getAsDouble(Double def) {
        return getAs(Double.class, def);
    }

    public Double getAsDouble() {
        return getAs(Double.class);
    }

    public Float getAsFloat(Float def) {
        return getAs(Float.class, def);
    }

    public Float getAsFloat() {
        return getAs(Float.class);
    }

    public Integer getAsInteger(Integer def) {
        return getAs(Integer.class, def);
    }

    public Integer getAsInteger() {
        return getAs(Integer.class);
    }

    public Long getAsLong(Long def) {
        return getAs(Long.class, def);
    }

    public Long getAsLong() {
        return getAs(Long.class);
    }

    public Boolean getAsBoolean(Boolean def) {
        return getAs(Boolean.class, def);
    }

    public Boolean getAsBoolean() {
        return getAs(Boolean.class);
    }

    public Short getAsShort(Short def) {
        return getAs(Short.class, def);
    }

    public Short getAsShort() {
        return getAs(Short.class);
    }

    public String getAsString(String def) {
        return getAs(String.class, def);
    }

    public String getAsString() {
        return getAs(String.class);
    }

    public Object getAsObject(Object def) {
        return getAs(Object.class, def);
    }

    public Object getAsObject() {
        return getAs(Object.class);
    }

    public UUID getAsUUID(UUID def) {
        return getAs(UUID.class, def);
    }

    public UUID getAsUUID() {
        return getAs(UUID.class);
    }

    public YamlContext getAsYamlContext(YamlContext def) {
        return getAs(YamlContext.class, def);
    }

    public YamlContext getAsYamlContext() {
        return getAs(YamlContext.class);
    }

    public @Nullable Object getValue() {
        return value;
    }
}
