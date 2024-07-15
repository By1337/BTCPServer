package org.by1337.btcp.common.util.id;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class SpacedName {
    private static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9-_]+$");
    private final String space;
    private final String name;

    public SpacedName(String space, String name) {
        validate(space, name);
        this.space = space;
        this.name = name;
    }

    public String asString() {
        return toString();
    }

    public static SpacedName parse(String input) {
        if (!input.contains(":")) throw new IllegalArgumentException("Expected <space>:<name>");
        String[] arr = input.split(":");
        if (arr.length != 2) throw new IllegalArgumentException("Expected <space>:<name>");
        return new SpacedName(arr[0], arr[1]);
    }

    public static void validate(String... inputs) {
        for (String input : inputs) {
            validate(input);
        }
    }

    public static void validate(String input) {
        validate(input, () -> String.format("Invalid name. Must be [a-zA-Z0-9._-]: '%s'", input));
    }

    public static void validate(String input, Supplier<String> message) {
        if (!pattern.matcher(input).matches()) {
            throw new IllegalArgumentException(message.get());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpacedName that = (SpacedName) o;
        return Objects.equals(space, that.space) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(space, name);
    }

    @Override
    public String toString() {
        return space + ":" + name;
    }
}
