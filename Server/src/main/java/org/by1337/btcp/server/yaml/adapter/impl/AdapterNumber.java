package org.by1337.btcp.server.yaml.adapter.impl;

import org.by1337.btcp.server.yaml.adapter.Adapter;

import java.util.function.Function;

public class AdapterNumber<T extends Number> implements Adapter<T> {
    private final Function<Number, T> cast;
    private final Function<String, T> parse;

    public AdapterNumber(Function<Number, T> cast, Function<String, T> parse) {
        this.cast = cast;
        this.parse = parse;
    }

    @Override
    public Object serialize(T raw) {
        return raw;
    }

    @Override
    public T deserialize(Object raw) {
        if (raw instanceof Number number) {
            return cast.apply(number);
        }
        return parse.apply(String.valueOf(raw));
    }
}
