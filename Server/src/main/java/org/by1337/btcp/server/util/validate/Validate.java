package org.by1337.btcp.server.util.validate;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Validate {

    public static <T, R> R tryCast(T raw, ThrowableFunction<T, R> mapper, String message, Object... objects) throws ClassCastException {
        return tryCast(raw, mapper, String.format(message, objects));
    }

    public static <T, R> R tryCast(T raw, ThrowableFunction<T, R> mapper, String message) throws ClassCastException {
        try {
            return mapper.apply(raw);
        } catch (Exception e) {
            throw new ClassCastException(message);
        }
    }
    @CanIgnoreReturnValue
    @NotNull
    public static <T> T notNull(@Nullable T obj, @NotNull String message, @NotNull Object... objects) {
        return notNull(obj, String.format(message, objects));
    }
    @CanIgnoreReturnValue
    @NotNull
    public static <T> T notNull(@Nullable T obj) {
        return notNull(obj, null);
    }

    @CanIgnoreReturnValue
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <T> T notNull(@Nullable T obj, @Nullable String message) {
        return test(obj, Objects::isNull, () -> new NullPointerException(message));
    }

    @CanIgnoreReturnValue
    public static <T, X extends Throwable> T test(@Nullable T obj, @NotNull Predicate<T> test, @NotNull Supplier<X> supplier) throws X {
        if (test.test(obj)) {
            throw supplier.get();
        }
        return obj;
    }

    public static <T, X extends Throwable, R extends Throwable> T invokeAndHandleException(
            Supplier<T> supplier,
            Class<X> throwableClass,
            ThrowableCreator<R> throwableCreator, String message)
            throws R {

        try {
            return supplier.get();
        } catch (Throwable t) {
            if (throwableClass.isAssignableFrom(t.getClass())) {
                throw throwableCreator.create(message, t);
            }
            throw t;
        }
    }

    @FunctionalInterface
    public interface ThrowableCreator<X extends Throwable> {
        X create(String message, Throwable cause);
    }
    @FunctionalInterface
    public interface ThrowableFunction<T, R> {

        /**
         * Applies this function to the given argument.
         *
         * @param t the function argument
         * @return the function result
         */
        R apply(T t) throws Exception;
    }
}
