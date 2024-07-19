package org.by1337.btcp.server.addon;

import org.by1337.btcp.server.yaml.YamlContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class AddonDescriptionFile {
    public static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9-_]+$");
    private final String name;
    private final String mainClass;
    private final String version;
    private final String description;
    private final Set<String> authors;
    private final Set<String> depend;
    private final Set<String> softDepend;

    public AddonDescriptionFile(String name, String mainClass, String version, String description, Set<String> authors, Set<String> depend, Set<String> softDepend) {
        this.name = name;
        validate(name);
        this.mainClass = mainClass;
        this.version = version;
        this.description = description;
        this.authors = authors;
        this.depend = depend;
        this.softDepend = softDepend;
    }

    public AddonDescriptionFile(YamlContext context) {
        this.name = Objects.requireNonNull(context.get("name").getAsString(), "missing 'name'!");
        validate(name);
        this.mainClass = Objects.requireNonNull(context.get("main").getAsString(), "missing 'main'!");
        this.version = context.get("version").getAsString("1.0");
        this.description = context.get("description").getAsString("");
        authors = new HashSet<>();
        if (context.get("authors").getAsObject() != null) {
            authors.addAll(context.get("authors").getAsList(String.class));
        }
        String author = context.get("author").getAsString();
        if (author != null) {
            authors.add(author);
        }
        depend = new HashSet<>();
        if (context.get("depend").getAsObject() != null) {
            depend.addAll(context.get("depend").getAsList(String.class));
        }
        softDepend = new HashSet<>();
        if (context.get("soft-depend").getAsObject() != null) {
            softDepend.addAll(context.get("soft-depend").getAsList(String.class));
        }
    }
    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getMain() {
        return mainClass;
    }

    @NotNull
    public String getVersion() {
        return version;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getAuthors() {
        return authors;
    }

    public static void validate(String input) {
        validate(input, () -> String.format("Invalid name. Must be [a-zA-Z0-9._-]: '%s'", input));
    }

    public static void validate(String input, Supplier<String> message) {
        if (!pattern.matcher(input).matches()) {
            throw new IllegalArgumentException(message.get());
        }
    }

    public Set<String> getDepend() {
        return depend;
    }

    public Set<String> getSoftDepend() {
        return softDepend;
    }
}
