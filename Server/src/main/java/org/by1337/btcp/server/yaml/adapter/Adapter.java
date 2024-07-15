package org.by1337.btcp.server.yaml.adapter;

public interface Adapter<T> {
    Object serialize(T raw);
    T deserialize(Object raw);
}
