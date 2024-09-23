package org.by1337.btcp.common.util.printer;

import io.netty.buffer.Unpooled;
import org.by1337.blib.nbt.NBT;
import org.by1337.blib.nbt.NbtType;
import org.by1337.blib.nbt.impl.CompoundTag;
import org.by1337.btcp.common.io.ByteBuffer;
import org.by1337.btcp.common.packet.PacketFlow;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ReadWriteMethodGeneratorTest {

    public static class ExamplePacket {
        private @ReadWriteMethodGenerator.NullSafe String id;
        private String password;
        private transient String secretKey;
    }

    public static class ExampleTestNBT {
        private NBT nbt;
        private CompoundTag compoundTag;

        private @ReadWriteMethodGenerator.NullSafe NBT nbtNull;
        private @ReadWriteMethodGenerator.NullSafe CompoundTag compoundTagNull;
    }

/*    @Test
    public void test() throws NoSuchFieldException, IOException {
        System.out.println(ReadWriteMethodGenerator.generate(ExampleTestNBT.class));
    }*/

    @Test
    public void generateTest() throws NoSuchFieldException {
        assertEquals(generateRead("id", ExamplePacket.class), "byteBuf.readOptional(ByteBuffer::readUtf).orElse(null)");
        assertEquals(generateWrite("id", ExamplePacket.class), "byteBuf.writeOptional(id, ByteBuffer::writeUtf)");
        assertEquals(generateWrite("password", ExamplePacket.class), "byteBuf.writeUtf(id)");
        assertEquals(generateRead("password", ExamplePacket.class), "byteBuf.readUtf()");

        assertEquals(generateWrite("nbt", ExampleTestNBT.class), "byteBuf.writeByte(id.getType().ordinal());id.write(byteBuf.asNBTByteBuffer())");
        assertEquals(generateRead("nbt", ExampleTestNBT.class), "NbtType.values()[byteBuf.readByte()].read(byteBuf.asNBTByteBuffer())");
        assertEquals(generateWrite("compoundTagNull", ExampleTestNBT.class), "byteBuf.writeOptional(id, (byteBufx, idx) -> {idx.write(byteBufx.asNBTByteBuffer());})");
        assertEquals(generateRead("compoundTagNull", ExampleTestNBT.class), "byteBuf.readOptional(byteBufx -> (CompoundTag) NbtType.COMPOUND.read(byteBufx.asNBTByteBuffer())).orElse(null)");

        String generated = ReadWriteMethodGenerator.generate(ExamplePacket.class);

        assertFalse(generated.contains("secretKey"));
    }

    private String generateRead(String field, Class<?> clazz) throws NoSuchFieldException {
        Field field1 = clazz.getDeclaredField(field);

        return ReadWriteMethodGenerator.generateRead(
                "byteBuf",
                "id",
                field1.getType(),
                false,
                field1.getAnnotatedType(),
                field1.isAnnotationPresent(ReadWriteMethodGenerator.NullSafe.class)
        );
    }

    private String generateWrite(String field, Class<?> clazz) throws NoSuchFieldException {
        Field field1 = clazz.getDeclaredField(field);

        return ReadWriteMethodGenerator.generateWrite(
                "byteBuf",
                "id",
                field1.getType(),
                false,
                field1.getAnnotatedType(),
                field1.isAnnotationPresent(ReadWriteMethodGenerator.NullSafe.class)
        );
    }

    @Test
    public void generatedMethodsTest() throws IOException {
        TestClass t = new TestClass();
        t.fill();

        ByteBuffer buffer = new ByteBuffer(Unpooled.buffer());
        t.write(buffer);

        TestClass t2 = new TestClass();
        t2.read(buffer);

        assertEquals(t, t2);

        buffer.release();
    }

    public static class TestClass {
        private int primitiveInt;
        private Integer integer;

        private Double aDouble;
        private double primitiveDouble;

        private Byte aByte;
        private byte primitiveByte;

        private Short aShort;
        private short primitiveShort;

        private Long aLong;
        private long primitiveLong;

        private Float aFloat;
        private float primitiveFloat;

        private String string;
        private @ReadWriteMethodGenerator.NullSafe String nullString;
        private UUID uuid;

        private PacketFlow packetFlow;

        private List<String> stringList;
        private List<@ReadWriteMethodGenerator.NullSafe String> nullStringList;
        private List<PacketFlow> packetFlowList;

        private List<List<String>> listListString;

        private Map<String, String> stringMap;
        private Map<String, PacketFlow> packetFlowMap;
        private Map<PacketFlow, String> packetFlowStringMap;

        private Map<String, Map<String, String>> mapStringMap;
        private Map<String, Map<String, Map<String, String>>> mapMapMapString;
        private Map<PacketFlow, Map<String, PacketFlow>> packetFlowMapMap;


        private void fill() {
            primitiveInt = 42;
            integer = 100;
            aDouble = 3.14;
            primitiveDouble = 2.718;
            aByte = 127;
            primitiveByte = 8;
            aShort = 30000;
            primitiveShort = 15000;
            aLong = 1234567890123L;
            primitiveLong = 9876543210L;
            aFloat = 1.23f;
            primitiveFloat = 4.56f;
            string = "Hello, World!";
            nullString = null;
            uuid = UUID.randomUUID();
            packetFlow = PacketFlow.ANY;
            stringList = Arrays.asList("One", "Two", "Three");
            nullStringList = Arrays.asList(null, "Test");
            packetFlowList = Arrays.asList(PacketFlow.ANY, PacketFlow.ANY);
            listListString = Arrays.asList(
                    Arrays.asList("Row1Col1", "Row1Col2"),
                    Arrays.asList("Row2Col1", "Row2Col2")
            );
            stringMap = new HashMap<>() {{
                put("Key1", "Value1");
                put("Key2", "Value2");
            }};
            packetFlowMap = new HashMap<>() {{
                put("Key1", PacketFlow.ANY);
                put("Key2", PacketFlow.SERVER_BOUND);
            }};
            packetFlowStringMap = new HashMap<>() {{
                put(PacketFlow.ANY, "Value1");
                put(PacketFlow.CLIENT_BOUND, "Value2");
            }};
            mapStringMap = new HashMap<>() {{
                put("OuterKey1", new HashMap<>() {{
                    put("InnerKey1", "InnerValue1");
                }});
            }};
            mapMapMapString = new HashMap<>() {{
                put("OuterKey1", new HashMap<>() {{
                    put("InnerKey1", new HashMap<>() {{
                        put("DeepInnerKey1", "DeepInnerValue1");
                    }});
                }});
            }};
            packetFlowMapMap = new HashMap<>() {{
                put(PacketFlow.ANY, new HashMap<>() {{
                    put("NestedKey1", PacketFlow.ANY);
                }});
            }};
        }

        /* generated */
        public void read(ByteBuffer byteBuf) throws IOException {
            primitiveInt = byteBuf.readVarInt();
            integer = byteBuf.readVarInt();
            aDouble = byteBuf.readDouble();
            primitiveDouble = byteBuf.readDouble();
            aByte = byteBuf.readByte();
            primitiveByte = byteBuf.readByte();
            aShort = byteBuf.readShort();
            primitiveShort = byteBuf.readShort();
            aLong = byteBuf.readVarLong();
            primitiveLong = byteBuf.readVarLong();
            aFloat = byteBuf.readFloat();
            primitiveFloat = byteBuf.readFloat();
            string = byteBuf.readUtf();
            nullString = byteBuf.readOptional(ByteBuffer::readUtf).orElse(null);
            uuid = byteBuf.readUUID();
            packetFlow = byteBuf.readEnum(PacketFlow.class);
            stringList = byteBuf.readList(ByteBuffer::readUtf);
            nullStringList = byteBuf.readList(byteBufx -> byteBufx.readOptional(ByteBuffer::readUtf).orElse(null));
            packetFlowList = byteBuf.readList(byteBufx -> byteBufx.readEnum(PacketFlow.class));
            listListString = byteBuf.readList(byteBufx -> byteBufx.readList(ByteBuffer::readUtf));
            stringMap = byteBuf.readMap(ByteBuffer::readUtf, ByteBuffer::readUtf);
            packetFlowMap = byteBuf.readMap(ByteBuffer::readUtf, byteBufx -> byteBufx.readEnum(PacketFlow.class));
            packetFlowStringMap = byteBuf.readMap(byteBufx -> byteBufx.readEnum(PacketFlow.class), ByteBuffer::readUtf);
            mapStringMap = byteBuf.readMap(ByteBuffer::readUtf, byteBufx -> byteBufx.readMap(ByteBuffer::readUtf, ByteBuffer::readUtf));
            mapMapMapString = byteBuf.readMap(ByteBuffer::readUtf, byteBufx -> byteBufx.readMap(ByteBuffer::readUtf, byteBufxx -> byteBufxx.readMap(ByteBuffer::readUtf, ByteBuffer::readUtf)));
            packetFlowMapMap = byteBuf.readMap(byteBufx -> byteBufx.readEnum(PacketFlow.class), byteBufx -> byteBufx.readMap(ByteBuffer::readUtf, byteBufxx -> byteBufxx.readEnum(PacketFlow.class)));
        }

        /* generated */
        public void write(ByteBuffer byteBuf) throws IOException {
            byteBuf.writeVarInt(primitiveInt);
            byteBuf.writeVarInt(integer);
            byteBuf.writeDouble(aDouble);
            byteBuf.writeDouble(primitiveDouble);
            byteBuf.writeByte(aByte);
            byteBuf.writeByte(primitiveByte);
            byteBuf.writeShort(aShort);
            byteBuf.writeShort(primitiveShort);
            byteBuf.writeVarLong(aLong);
            byteBuf.writeVarLong(primitiveLong);
            byteBuf.writeFloat(aFloat);
            byteBuf.writeFloat(primitiveFloat);
            byteBuf.writeUtf(string);
            byteBuf.writeOptional(nullString, ByteBuffer::writeUtf);
            byteBuf.writeUUID(uuid);
            byteBuf.writeEnum(packetFlow);
            byteBuf.writeList(stringList, ByteBuffer::writeUtf);
            byteBuf.writeList(nullStringList, (byteBufx, nullStringListx) -> byteBufx.writeOptional(nullStringListx, ByteBuffer::writeUtf));
            byteBuf.writeList(packetFlowList, ByteBuffer::writeEnum);
            byteBuf.writeList(listListString, (byteBufx, listListStringx) -> byteBufx.writeList(listListStringx, ByteBuffer::writeUtf));
            byteBuf.writeMap(stringMap, ByteBuffer::writeUtf, ByteBuffer::writeUtf);
            byteBuf.writeMap(packetFlowMap, ByteBuffer::writeUtf, ByteBuffer::writeEnum);
            byteBuf.writeMap(packetFlowStringMap, ByteBuffer::writeEnum, ByteBuffer::writeUtf);
            byteBuf.writeMap(mapStringMap, ByteBuffer::writeUtf, (byteBufx, mapStringMapx) -> byteBufx.writeMap(mapStringMapx, ByteBuffer::writeUtf, ByteBuffer::writeUtf));
            byteBuf.writeMap(mapMapMapString, ByteBuffer::writeUtf, (byteBufx, mapMapMapStringx) -> byteBufx.writeMap(mapMapMapStringx, ByteBuffer::writeUtf, (byteBufxx, mapMapMapStringxx) -> byteBufxx.writeMap(mapMapMapStringxx, ByteBuffer::writeUtf, ByteBuffer::writeUtf)));
            byteBuf.writeMap(packetFlowMapMap, ByteBuffer::writeEnum, (byteBufx, packetFlowMapMapx) -> byteBufx.writeMap(packetFlowMapMapx, ByteBuffer::writeUtf, ByteBuffer::writeEnum));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestClass testClass = (TestClass) o;
            return primitiveInt == testClass.primitiveInt && Double.compare(primitiveDouble, testClass.primitiveDouble) == 0 && primitiveByte == testClass.primitiveByte && primitiveShort == testClass.primitiveShort && primitiveLong == testClass.primitiveLong && Float.compare(primitiveFloat, testClass.primitiveFloat) == 0 && Objects.equals(integer, testClass.integer) && Objects.equals(aDouble, testClass.aDouble) && Objects.equals(aByte, testClass.aByte) && Objects.equals(aShort, testClass.aShort) && Objects.equals(aLong, testClass.aLong) && Objects.equals(aFloat, testClass.aFloat) && Objects.equals(string, testClass.string) && Objects.equals(nullString, testClass.nullString) && Objects.equals(uuid, testClass.uuid) && packetFlow == testClass.packetFlow && Objects.equals(stringList, testClass.stringList) && Objects.equals(nullStringList, testClass.nullStringList) && Objects.equals(packetFlowList, testClass.packetFlowList) && Objects.equals(listListString, testClass.listListString) && Objects.equals(stringMap, testClass.stringMap) && Objects.equals(packetFlowMap, testClass.packetFlowMap) && Objects.equals(packetFlowStringMap, testClass.packetFlowStringMap) && Objects.equals(mapStringMap, testClass.mapStringMap) && Objects.equals(mapMapMapString, testClass.mapMapMapString) && Objects.equals(packetFlowMapMap, testClass.packetFlowMapMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(primitiveInt, integer, aDouble, primitiveDouble, aByte, primitiveByte, aShort, primitiveShort, aLong, primitiveLong, aFloat, primitiveFloat, string, nullString, uuid, packetFlow, stringList, nullStringList, packetFlowList, listListString, stringMap, packetFlowMap, packetFlowStringMap, mapStringMap, mapMapMapString, packetFlowMapMap);
        }
    }

}