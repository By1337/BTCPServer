package org.by1337.btcp.server.yaml.adapter;

import java.util.function.Function;

public class AdapterBuilder<T> {
    private Function<T, Object> serialize;
    private Function<Object, T> deserialize;

    public AdapterBuilder<T> serialize(Function<T, Object> serialize) {
        this.serialize = serialize;
        return this;
    }

    public AdapterBuilder<T> deserialize(Function<Object, T> deserialize) {
        this.deserialize = deserialize;
        return this;
    }

    public Adapter<T> build() {
        if (serialize == null) {
            serialize = o -> o;
        }
        if (deserialize == null) {
            deserialize = o -> (T) o;
        }
        return new Adapter<>() {
            @Override
            public Object serialize(T raw) {
                return serialize.apply(raw);
            }

            @Override
            public T deserialize(Object raw) {
                return deserialize.apply(raw);
            }
        };
    }
}
