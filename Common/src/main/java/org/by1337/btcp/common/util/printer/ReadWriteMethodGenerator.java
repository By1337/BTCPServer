package org.by1337.btcp.common.util.printer;

import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.Packet;
import org.by1337.btcp.common.util.id.SpacedName;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * This class automatically generates {@code read(ByteBuffer byteBuf)} and {@code write(ByteBuffer byteBuf)} methods
 * for your class fields to simplify serialization and deserialization with the ByteBuffer format.
 * <p>
 * Key Features:
 * <p>
 * 1. Automatic Method Generation:
 * <ul>
 *   <li> Just call one method:
 *   <li> {@code System.out.println(ReadWriteMethodGenerator.generate(YourClass.class));}
 *   <li> This method will output ready-to-use `read` and `write` methods for the specified class.
 * </ul>
 * <p>
 * 2. Annotation Handling:
 * <ul>
 *   <li> The class supports fields annotated with {@link NullSafe} and automatically wraps such fields
 *   <li> with {@link ByteBuffer#readOptional} and {@link ByteBuffer#writeOptional} methods to properly handle {@code null} values.
 * </ul>
 * <p>
 *    Important: Since it is not possible to check for the {@link Nullable} annotation at runtime,
 *    use the {@link NullSafe} annotation during method generation. After generation is complete,
 *    you can revert to using the {@link Nullable} annotation if needed.
 * <p>
 * 3. Field Ignoring:
 * <ul>
 *   <li> Fields marked as {@code transient} will be excluded from the generated `read` and `write` methods,
 *   <li> allowing you to omit temporary data.
 * </ul>
 * <p>
 * Example:
 * <p>
 * For the following class:
 * <p>
 * <pre> {@code
 * public static class ExamplePacket {
 *     private @ReadWriteMethodGenerator.NullSafe String id;
 *     private String password;
 *     private transient String secretKey;
 * }
 * }</pre>
 * the following code will be generated:
 * <pre> {@code
 * public void read(ByteBuffer byteBuf) throws IOException {
 *     id = byteBuf.readOptional(ByteBuffer::readUtf).orElse(null);
 *     password = byteBuf.readUtf();
 * }
 *
 * public void write(ByteBuffer byteBuf) throws IOException {
 *     byteBuf.writeOptional(id, ByteBuffer::writeUtf);
 *     byteBuf.writeUtf(password);
 * }
 * }</pre>
 *
 * @see NullSafe
 */
public class ReadWriteMethodGenerator {

    /**
     * The {@code NullSafe} annotation is used to indicate fields that can hold a {@code null} value.
     * Fields annotated with this annotation will be handled by the special methods
     * {@link ByteBuffer#readOptional} and {@link ByteBuffer#writeOptional}
     * to correctly manage {@code null} values when reading from or writing to a {@link ByteBuffer}.
     * <p>
     * The use of this annotation is temporarily necessary, as it's not possible to check for the
     * {@link Nullable} annotation at runtime. Once methods are generated, the {@link Nullable}
     * annotation can be re-applied if needed.
     * <p>
     * Example:
     * <pre> {@code
     * ReadWriteMethodGenerator.NullSafe
     * private String id;
     * }</pre>
     * <p>
     * In the generated method, {@code byteBuf.readOptional(...)} will be used for reading, and
     * {@code byteBuf.writeOptional(...)} for writing the value, ensuring proper handling of potential {@code null}.
     *
     * @see Nullable
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
    public @interface NullSafe {
    }

    private static final Map<Class<?>, Printer> printers = new HashMap<>();

    public static String generate(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        StringBuilder read = new StringBuilder("public void read(ByteBuffer byteBuf) throws IOException {\n");
        StringBuilder write = new StringBuilder("public void write(ByteBuffer byteBuf) throws IOException {\n");

        for (Field field : fields) {
            if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) continue;
            Class<?> type = field.getType();

            read.append("    ").append(field.getName()).append(" = ")
                    .append(generateRead("byteBuf", field.getName(), type, false, field.getAnnotatedType(), field.isAnnotationPresent(NullSafe.class)))
                    .append(";\n");
            write.append("    ")
                    .append(generateWrite("byteBuf", field.getName(), type, false, field.getAnnotatedType(), field.isAnnotationPresent(NullSafe.class)))
                    .append(";\n");

        }
        read.append("}\n");
        write.append("}\n");
        return read.append(write).toString();
    }

    @VisibleForTesting
    static String generateRead(String buffer, String field, Class<?> type, boolean lambda, @Nullable AnnotatedType genericType, boolean nullSafe) {
        if (nullSafe) {
            String buffer0 = buffer + "x";
            if (lambda) {
                return buffer + " -> " + buffer + ".readOptional(" + generateRead(buffer0, field, type, true, genericType, false) + ").orElse(null)";
            } else {
                return buffer + ".readOptional(" + generateRead(buffer0, field, type, true, genericType, false) + ").orElse(null)";
            }
        }
        final Printer printer;
        if (type.isEnum()) {
            printer = printers.get(Enum.class);
        } else {
            printer = printers.get(type);
        }
        if (printer == null) {
            try {
                if (genericType instanceof AnnotatedParameterizedType annotatedParameterizedType) {
                    if (annotatedParameterizedType.getAnnotatedActualTypeArguments().length == 1) {
                        if (List.class.isAssignableFrom(type)) {
                            return OneGenericPrinter.print(buffer, field, type, lambda, annotatedParameterizedType, "readList", true);
                        } else if (Optional.class.isAssignableFrom(type)) {
                            return OneGenericPrinter.print(buffer, field, type, lambda, annotatedParameterizedType, "readOptional", true);
                        }
                    } else if (annotatedParameterizedType.getAnnotatedActualTypeArguments().length == 2) {
                        if (Map.class.isAssignableFrom(type)) {
                            return MapPrinter.print(buffer, field, type, lambda, annotatedParameterizedType, "readMap", true);
                        }
                    }
                }
            } catch (IllegalArgumentException ignore) {
            }
            return String.format("/* can't read %s type: %s */", field, type.getCanonicalName());
        }
        if (lambda) {
            return printer.printReadLambda(buffer, type);
        } else {
            return printer.printRead(buffer, type);
        }
    }

    @VisibleForTesting
    static String generateWrite(String buffer, String field, Class<?> type, boolean lambda, @Nullable AnnotatedType genericType, boolean nullSafe) {
        if (nullSafe) {
            String buffer0 = buffer + "x";
            if (lambda) {
                String field0 = field + "x";
                return "(" + buffer + ", " + field0 + ") -> " + buffer + ".writeOptional(" + field0 + ", " + generateWrite(buffer0, field0, type, true, genericType, false) + ")";
            } else {
                return buffer + ".writeOptional(" + field + ", " + generateWrite(buffer0, field, type, true, genericType, false) + ")";
            }
        }
        Printer printer;
        if (type.isEnum()) {
            printer = printers.get(Enum.class);
        } else {
            printer = printers.get(type);
        }
        if (printer == null) {
            try {
                if (genericType instanceof AnnotatedParameterizedType annotatedParameterizedType) {
                    if (annotatedParameterizedType.getAnnotatedActualTypeArguments().length == 1) {
                        if (List.class.isAssignableFrom(type)) {
                            return OneGenericPrinter.print(buffer, field, type, lambda, annotatedParameterizedType, "writeList", false);
                        } else if (Optional.class.isAssignableFrom(type)) {
                            return OneGenericPrinter.print(buffer, field, type, lambda, annotatedParameterizedType, "writeOptional", false);
                        }
                    } else if (annotatedParameterizedType.getAnnotatedActualTypeArguments().length == 2) {
                        if (Map.class.isAssignableFrom(type)) {
                            return MapPrinter.print(buffer, field, type, lambda, annotatedParameterizedType, "writeMap", false);
                        }
                    }
                }
            } catch (IllegalArgumentException ignore) {
            }
            return String.format("/* can't write %s type: %s */", field, type.getCanonicalName());
        }
        if (lambda) {
            return printer.printWriteLambda(buffer, field + "x", type);
        } else {
            return printer.printWrite(buffer, field, type);
        }
    }


    static {
        printers.put(Integer.class, new DefaultPrinter("readVarInt", "writeVarInt"));
        printers.put(int.class, printers.get(Integer.class));

        printers.put(Long.class, new DefaultPrinter("readVarLong", "writeVarLong"));
        printers.put(long.class, printers.get(Long.class));

        printers.put(Double.class, new DefaultPrinter("readDouble", "writeDouble"));
        printers.put(double.class, printers.get(Double.class));

        printers.put(Short.class, new DefaultPrinter("readShort", "writeShort"));
        printers.put(short.class, printers.get(Short.class));

        printers.put(Boolean.class, new DefaultPrinter("readBoolean", "writeBoolean"));
        printers.put(boolean.class, printers.get(Boolean.class));

        printers.put(Byte.class, new DefaultPrinter("readByte", "writeByte"));
        printers.put(byte.class, printers.get(Byte.class));

        printers.put(Float.class, new DefaultPrinter("readFloat", "writeFloat"));
        printers.put(float.class, printers.get(Float.class));

        printers.put(UUID.class, new DefaultPrinter("readUUID", "writeUUID"));
        printers.put(String.class, new DefaultPrinter("readUtf", "writeUtf"));
        printers.put(Packet.class, new DefaultPrinter("readPacket", "writePacket"));
        printers.put(SpacedName.class, new DefaultPrinter("readSpacedName", "writeSpacedName"));

        printers.put(Enum.class, new EnumPrinter());
    }

    private interface Printer {
        String printRead(String buffer, Class<?> type);

        String printReadLambda(String buffer, Class<?> type);

        String printWrite(String buffer, String field, Class<?> type);

        String printWriteLambda(String buffer, String field, Class<?> type);
    }

    private static class DefaultPrinter implements Printer {

        private final String readMethod;
        private final String writeMethod;

        public DefaultPrinter(String readMethod, String writeMethod) {
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
        }

        @Override
        public String printRead(String buffer, Class<?> type) {
            return buffer + "." + readMethod + "()";
        }

        @Override
        public String printReadLambda(String buffer, Class<?> type) {
            return "ByteBuffer::" + readMethod;
        }

        @Override
        public String printWrite(String buffer, String field, Class<?> type) {
            return buffer + "." + writeMethod + "(" + field + ")";
        }

        @Override
        public String printWriteLambda(String buffer, String field, Class<?> type) {
            return "ByteBuffer::" + writeMethod;
        }
    }

    private static class EnumPrinter implements Printer {

        @Override
        public String printRead(String buffer, Class<?> type) {
            return buffer + ".readEnum(" + type.getSimpleName() + ".class)";
        }

        @Override
        public String printReadLambda(String buffer, Class<?> type) {
            return buffer + " -> " + buffer + ".readEnum(" + type.getSimpleName() + ".class)";
        }

        @Override
        public String printWrite(String buffer, String field, Class<?> type) {
            return buffer + ".writeEnum(" + field + ")";
        }

        @Override
        public String printWriteLambda(String buffer, String field, Class<?> type) {
            return "ByteBuffer::writeEnum";
        }
    }

    private static class OneGenericPrinter {

        public static String print(String buffer, String field, Class<?> type, boolean lambda, AnnotatedParameterizedType parameterizedType, String method, boolean read) {
            if (parameterizedType.getAnnotatedActualTypeArguments().length != 1) throw new IllegalArgumentException();
            String buffer0 = buffer + "x";
            AnnotatedType t = parameterizedType.getAnnotatedActualTypeArguments()[0];
            Class<?> type0 = getType(t);

            StringBuilder sb = new StringBuilder();


            if (read) {
                if (lambda) {
                    sb.append(buffer).append(" -> ");
                }
                sb.append(buffer).append(".").append(method).append("(")
                        .append(generateRead(buffer0, field, type0, true, t, t.isAnnotationPresent(NullSafe.class)))
                        .append(")");
            } else {
                String field0;
                if (lambda) {
                    field0 = field + "x";
                    sb.append("(").append(buffer).append(", ").append(field0).append(") -> ");
                } else {
                    field0 = field;
                }

                sb.append(buffer).append(".").append(method).append("(")
                        .append(field0).append(", ")
                        .append(generateWrite(buffer0, field0, type0, true, t, t.isAnnotationPresent(NullSafe.class)))
                        .append(")");
            }
            return sb.toString();
        }

    }

    private static class MapPrinter {

        public static String print(String buffer, String field, Class<?> type, boolean lambda, AnnotatedParameterizedType parameterizedType, String method, boolean read) {
            if (parameterizedType.getAnnotatedActualTypeArguments().length != 2) throw new IllegalArgumentException();
            String buffer0 = buffer + "x";
            AnnotatedType param1 = parameterizedType.getAnnotatedActualTypeArguments()[0];
            AnnotatedType param2 = parameterizedType.getAnnotatedActualTypeArguments()[1];

            Class<?> type1 = getType(param1);
            Class<?> type2 = getType(param2);

            StringBuilder sb = new StringBuilder();

            if (read) {
                if (lambda) {
                    sb.append(buffer).append(" -> ");
                }
                sb.append(buffer).append(".").append(method).append("(")
                        .append(generateRead(buffer0, field, type1, true, param1, param1.isAnnotationPresent(NullSafe.class)))
                        .append(", ")
                        .append(generateRead(buffer0, field, type2, true, param2, param2.isAnnotationPresent(NullSafe.class)))
                        .append(")");
            } else {
                String field0;

                if (lambda) {
                    field0 = field + "x";
                    sb.append("(").append(buffer).append(", ").append(field0).append(") -> ");
                } else {
                    field0 = field;
                }
                sb.append(buffer).append(".").append(method).append("(")
                        .append(field0).append(", ")
                        .append(generateWrite(buffer0, field0, type1, true, param1, param1.isAnnotationPresent(NullSafe.class)))
                        .append(", ")
                        .append(generateWrite(buffer0, field0, type2, true, param2, param2.isAnnotationPresent(NullSafe.class)))
                        .append(")");
            }
            return sb.toString();
        }


    }

    private static Class<?> getType(AnnotatedType type) {
        if (type.getType() instanceof Class<?> clazz) {
            return clazz;
        } else if (type instanceof AnnotatedParameterizedType parameterizedType1) {
            if (parameterizedType1.getType() instanceof Class<?> clazz) {
                return clazz;
            } else if (parameterizedType1.getType() instanceof ParameterizedType parameterizedType2) {
                if (parameterizedType2.getRawType() instanceof Class<?> clazz) {
                    return clazz;
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
}
