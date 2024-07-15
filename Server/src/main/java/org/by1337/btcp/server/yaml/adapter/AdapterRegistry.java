package org.by1337.btcp.server.yaml.adapter;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.by1337.btcp.server.util.validate.Validate;
import org.by1337.btcp.server.yaml.YamlContext;
import org.by1337.btcp.server.yaml.adapter.impl.AdapterEnum;
import org.by1337.btcp.server.yaml.adapter.impl.AdapterNumber;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

public class AdapterRegistry {
    private static final HashMap<Class<?>, Adapter<?>> adapters;

    @CanIgnoreReturnValue
    public static <T> boolean registerAdapter(Class<T> adapterClass, Adapter<? extends T> adapter) {
        if (hasAdapter(adapterClass)) {
            return false;
        }
        adapters.put(adapterClass, adapter);
        return true;
    }

    @CanIgnoreReturnValue
    public static <T> boolean unregisterAdapter(Class<T> adapterClass) {
        if (!hasAdapter(adapterClass)) {
            return false;
        }
        adapters.remove(adapterClass);
        return true;
    }

    @Contract("null, _ -> null")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T getAs(@Nullable Object o, @NotNull Class<T> clazz) {
        if (o == null) return null;
        if (clazz.isAssignableFrom(o.getClass())) {
            return clazz.cast(o);
        }
        Validate.notNull(clazz, "class is null!");
        Adapter<T> adapter = (Adapter<T>) adapters.get(clazz);
        if (adapter == null) {
            if (clazz.isEnum()) {
                AdapterEnum adapterEnum = new AdapterEnum(clazz);
                adapters.put(clazz, adapterEnum);
                return (T) adapterEnum.deserialize(o);
            }
        }
        Validate.notNull(adapter, "Has no adapter for %s class!", clazz);
        return adapter.deserialize(o);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> Object serialize(T o) {
        if (o == null) return null;
        Adapter<T> adapter = (Adapter<T>) adapters.get(o.getClass());
        if (adapter == null) {
            if (o.getClass().isEnum()) {
                AdapterEnum adapterEnum = new AdapterEnum(o.getClass());
                adapters.put(o.getClass(), adapterEnum);
                return adapterEnum.serialize((Enum) o);
            }
        }
        Validate.notNull(adapter, "Has no adapter for %s class!", o.getClass());
        return adapter.serialize(o);
    }

    public static boolean hasAdapter(Class<?> adapterClass) {
        return adapters.containsKey(adapterClass);
    }

    static {
        adapters = new HashMap<>();

        registerAdapter(Object.class, new AdapterBuilder<>().build());
        registerAdapter(String.class, new AdapterBuilder<String>().deserialize(String::valueOf).build());

        registerAdapter(Number.class, new AdapterNumber<>(n -> n, Double::parseDouble));
        registerAdapter(Byte.class, new AdapterNumber<>(Number::byteValue, Byte::parseByte));
        registerAdapter(Short.class, new AdapterNumber<>(Number::shortValue, Short::parseShort));
        registerAdapter(Integer.class, new AdapterNumber<>(Number::intValue, Integer::parseInt));
        registerAdapter(Long.class, new AdapterNumber<>(Number::longValue, Long::parseLong));
        registerAdapter(Double.class, new AdapterNumber<>(Number::doubleValue, Double::parseDouble));
        registerAdapter(Float.class, new AdapterNumber<>(Number::floatValue, Float::parseFloat));

        registerAdapter(Boolean.class, new AdapterBuilder<Boolean>().deserialize(o -> {
            if (o instanceof Boolean b) return b;
            return Boolean.parseBoolean(String.valueOf(o));
        }).build());


        registerAdapter(UUID.class, new AdapterBuilder<UUID>()
                .serialize(UUID::toString)
                .deserialize(o -> UUID.fromString(String.valueOf(o)))
                .build());

        registerAdapter(YamlContext.class, new AdapterBuilder<YamlContext>()
                .serialize(YamlContext::getRaw)
                .deserialize(o -> new YamlContext((LinkedHashMap<String, Object>) o))
                .build());


    }
}
