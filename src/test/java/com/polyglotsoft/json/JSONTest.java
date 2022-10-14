package com.polyglotsoft.json;

import org.junit.Test;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class JSONTest {
    public static class SingleFieldClass {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class MultiFieldClass {
        private String name;
        private String type;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class EmbeddedClass {
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class ContainerClass {
        private String name;
        private EmbeddedClass embedded;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public EmbeddedClass getEmbedded() {
            return embedded;
        }

        public void setEmbedded(EmbeddedClass embedded) {
            this.embedded = embedded;
        }
    }

    private static class ExtendedEmbeddedClass extends EmbeddedClass {
        private String description;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class ArrayContainerClass {
        private String name;
        private EmbeddedClass[] embeddedArray;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public EmbeddedClass[] getEmbeddedArray() {
            return embeddedArray;
        }

        public void setEmbeddedArray(EmbeddedClass[] embeddedArray) {
            this.embeddedArray = embeddedArray;
        }
    }

    @Test
    public void shouldConvertPrimitivesToJSON() {
        assertEquals("null", JSON.toJSON(null));
        assertEquals("\"\"", JSON.toJSON(""));

        assertEquals("true", JSON.toJSON(true));
        assertEquals("A", JSON.toJSON((char) 65));
        assertEquals("-1", JSON.toJSON((byte) 255));
        assertEquals("1", JSON.toJSON((short) 1));
        assertEquals("2", JSON.toJSON(2));
        assertEquals("3", JSON.toJSON(3L));

        assertEquals("4.5", JSON.toJSON(4.5F));
        assertEquals("6.7", JSON.toJSON(6.7D));
    }

    @Test
    public void shouldConvertEmptyArrayToJSON() {
        assertEquals("[]", JSON.toJSON(new int[0]));
    }

    @Test
    public void shouldConvertPrimitiveArrayToJSON() {
        assertEquals("[null,null]", JSON.toJSON(new Object[] { null, null }));
        assertEquals("[true,false]", JSON.toJSON(new boolean[] { true, false }));
        assertEquals("[A,B]", JSON.toJSON(new char[] { 65, 66 }));
        assertEquals("[-1,-2]", JSON.toJSON(new byte[] { (byte) 255, (byte) 254 }));
        assertEquals("[1,2]", JSON.toJSON(new short[] { (short) 1, (short) 2 }));
        assertEquals("[3,4]", JSON.toJSON(new int[] { 3, 4 }));
        assertEquals("[5,6]", JSON.toJSON(new long[] { 5L, 6L }));
        assertEquals("[7.8,8.9]", JSON.toJSON(new float[] { 7.8F, 8.9F }));
        assertEquals("[9.1,9.2]", JSON.toJSON(new double[] { 9.1D, 9.2D }));
    }

    @Test
    public void shouldConvertMixedArrayToJSON() {
        assertEquals("[1,\"2\",-3,4,5,6,7.1,8.2]",
                JSON.toJSON(new Object[] { 1, "2", (byte) 253, (short) 4, 5, 6L, 7.1F, 8.2D }));
    }

    @Test
    public void shouldConvertEmptyObjectToJSON() {
        assertEquals("{}", JSON.toJSON(new Object()));
    }

    @Test
    public void shouldConvertObjectToJSON() {
        SingleFieldClass singleFieldObject = new SingleFieldClass();

        singleFieldObject.setName("value");

        assertEquals("{\"name\":\"value\"}", JSON.toJSON(singleFieldObject));

        MultiFieldClass multiFieldObject = new MultiFieldClass();

        multiFieldObject.setName("value");
        multiFieldObject.setType("standard");

        assertEquals("{\"name\":\"value\",\"type\":\"standard\"}", JSON.toJSON(multiFieldObject));
    }

    @Test
    public void shouldConvertEmbeddedObjectToJSON() {
        ContainerClass containerObject = new ContainerClass();
        EmbeddedClass embeddedObject = new EmbeddedClass();

        embeddedObject.setType("standard");

        containerObject.setName("value");
        containerObject.setEmbedded(embeddedObject);

        assertEquals("{\"embedded\":{\"type\":\"standard\"},\"name\":\"value\"}",
                JSON.toJSON(containerObject));

        ArrayContainerClass arrayContainerObject = new ArrayContainerClass();
        ExtendedEmbeddedClass extendedEmbeddedObject = new ExtendedEmbeddedClass();

        extendedEmbeddedObject.setType("extended");
        extendedEmbeddedObject.setDescription("some description");

        arrayContainerObject.setEmbeddedArray(new EmbeddedClass[] { embeddedObject, extendedEmbeddedObject });
        arrayContainerObject.setName("value");

        assertEquals(""
            + "{"
            + "" + "\"embeddedArray\":[{"
            + "" + "" + "\"type\":\"standard\""
            + "" + "},{"
            + "" + "" + "\"description\":\"some description\","
            + "" + "" + "\"type\":\"extended\""
            + "" + "}],"
            + "" + "\"name\":\"value\""
            + "}", JSON.toJSON(arrayContainerObject));
    }

    @Test
    public void shouldConvertJSONToPrimitive() {
        assertTrue(JSON.toBoolean("true"));
        assertFalse(JSON.toBoolean("false"));
        assertFalse(JSON.toBoolean("invalid json"));
        assertEquals((byte) 255, JSON.toByte("-1"));
        assertEquals((short) 1, JSON.toShort("1"));
        assertEquals(2, JSON.toInt("2"));
        assertEquals(3L, JSON.toLong("3"));

        assertEquals(4.5F, JSON.toFloat("4.5"), 0.0F);
        assertEquals(6.7D, JSON.toDouble("6.7"), 0.0D);
    }

    @Test
    public void shouldConvertJSONToPrimitiveAtLimits() {
        assertEquals(Byte.MIN_VALUE, JSON.toByte(String.valueOf(Byte.MIN_VALUE)));
        assertEquals(Byte.MAX_VALUE, JSON.toByte(String.valueOf(Byte.MAX_VALUE)));

        assertEquals(Short.MIN_VALUE, JSON.toShort(String.valueOf(Short.MIN_VALUE)));
        assertEquals(Short.MAX_VALUE, JSON.toShort(String.valueOf(Short.MAX_VALUE)));

        assertEquals(Integer.MIN_VALUE, JSON.toInt(String.valueOf(Integer.MIN_VALUE)));
        assertEquals(Integer.MAX_VALUE, JSON.toInt(String.valueOf(Integer.MAX_VALUE)));

        assertEquals(Long.MIN_VALUE, JSON.toLong(String.valueOf(Long.MIN_VALUE)));
        assertEquals(Long.MAX_VALUE, JSON.toLong(String.valueOf(Long.MAX_VALUE)));

        assertEquals(Float.MIN_VALUE, JSON.toFloat(String.valueOf(Float.MIN_VALUE)), 0.0F);
        assertEquals(Float.MAX_VALUE, JSON.toFloat(String.valueOf(Float.MAX_VALUE)), 0.0F);
        assertEquals(Double.MIN_VALUE, JSON.toDouble(String.valueOf(Double.MIN_VALUE)), 0.0D);
        assertEquals(Double.MAX_VALUE, JSON.toDouble(String.valueOf(Double.MAX_VALUE)), 0.0D);
    }

    @Test
    public void shouldConvertJSONToString() {
        assertEquals("value", JSON.toString("\"value\""));
    }

    @Test
    public void shouldConvertJSONArrayOfPrimitivesToArray() {
        assertArrayEquals(new boolean[] { true, false }, JSON.toBooleanArray("[true,false]"));
        assertArrayEquals(new byte[] { (byte) 255, (byte) 254 }, JSON.toByteArray("[-1,-2]"));
        assertArrayEquals(new short[] { (short) 1, (short) 2 }, JSON.toShortArray("[1,2]"));
        assertArrayEquals(new int[] { 3, 4 }, JSON.toIntArray("[3,4]"));
        assertArrayEquals(new long[] { 5L, 6L }, JSON.toLongArray("[5,6]"));
        assertArrayEquals(new float[] { 7.7F, 8.8F }, JSON.toFloatArray("[7.7,8.8]"), 0.0F);
        assertArrayEquals(new double[] { 9.9D, 1.0D }, JSON.toDoubleArray("[9.9,1.0]"), 0.0D);
    }

    @Test
    public void shouldConvertJSONToObject() throws IllegalAccessException, IntrospectionException, InvocationTargetException {
        Object result = JSON.toObject(SingleFieldClass.class, "{\"name\":\"value\"}");

        assertNotNull(result);
        assertEquals("value",
                Introspector.getBeanInfo(result.getClass()).getPropertyDescriptors()[1].getReadMethod().invoke(result));

        result = JSON.toObject(MultiFieldClass.class, "{\"name\":\"value\",\"type\":\"standard\"}");

        assertNotNull(result);
        assertEquals("value",
                Introspector.getBeanInfo(result.getClass()).getPropertyDescriptors()[1].getReadMethod().invoke(result));
        assertEquals("standard",
                Introspector.getBeanInfo(result.getClass()).getPropertyDescriptors()[2].getReadMethod().invoke(result));
    }

    @Test
    public void shouldConvertEmbeddedJSONToObject() throws IllegalAccessException, IntrospectionException, InvocationTargetException {
        Object result = JSON.toObject(ContainerClass.class, "{\"embedded\":{\"type\":\"standard\"},\"name\":\"value\"}");

        assertNotNull(result);

        Object embedded = Introspector.getBeanInfo(result.getClass()).getPropertyDescriptors()[1].getReadMethod().invoke(result);

        assertEquals("standard",
                Introspector.getBeanInfo(embedded.getClass()).getPropertyDescriptors()[1].getReadMethod().invoke(embedded));
        assertEquals("value",
                Introspector.getBeanInfo(result.getClass()).getPropertyDescriptors()[2].getReadMethod().invoke(result));
    }

    @Test
    public void shouldParseEmptyObjectArray() {
        List<Map<String, Object>> result = JSON.toObjectArray("[]");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldParseEmptyDoubleArray() {
        double[] result = JSON.toDoubleArray("[]");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyLongArray() {
        long[] result = JSON.toLongArray("[]");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyStringArray() {
        String[] result = JSON.toStringArray("[]");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyStringArrayWithWhitespace() {
        String[] result = JSON.toStringArray(" [ ] ");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyBooleanArray() {
        boolean[] result = JSON.toBooleanArray("[]");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyBooleanArrayWithWhitespace() {
        boolean[] result = JSON.toBooleanArray(" [ ] ");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyByteArray() {
        byte[] result = JSON.toByteArray("[]");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyByteArrayWithWhitespace() {
        byte[] result = JSON.toByteArray(" [ ] ");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyFloatArray() {
        float[] result = JSON.toFloatArray("[]");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyFloatArrayWithWhitespace() {
        float[] result = JSON.toFloatArray(" [ ] ");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyIntArray() {
        int[] result = JSON.toIntArray("[]");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyIntArrayWithWhitespace() {
        int[] result = JSON.toIntArray(" [ ] ");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyShortArray() {
        short[] result = JSON.toShortArray("[]");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyShortArrayWithWhitespace() {
        short[] result = JSON.toShortArray(" [ ] ");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void shouldParseEmptyObject() {
        SingleFieldClass result = JSON.toObject(SingleFieldClass.class, "{}");

        assertNotNull(result);
        assertNull(result.getName());
    }

    @Test
    public void shouldParseEmptyObjectWithWhitespace() {
        SingleFieldClass result = JSON.toObject(SingleFieldClass.class, " { } ");

        assertNotNull(result);
        assertNull(result.getName());
    }

    @Test
    public void shouldParseSingleFieldClassObject() {
        SingleFieldClass result = JSON.toObject(SingleFieldClass.class, "{\"name\":\"123\"}");

        assertNotNull(result);
        assertEquals("123", result.getName());
    }

    @Test
    public void shouldParseSingleFieldClassObjectWithWhitespace() {
        SingleFieldClass result = JSON.toObject(SingleFieldClass.class, " { \"name\" : \"123\" } ");

        assertNotNull(result);
        assertEquals("123", result.getName());
    }

    @Test
    public void shouldParseMultiFieldClassObject() {
        MultiFieldClass result = JSON.toObject(MultiFieldClass.class, "{\"name\":\"123\",\"type\":\"1\"}");

        assertNotNull(result);
        assertEquals("123", result.getName());
        assertEquals("1", result.getType());
    }

    @Test
    public void shouldParseMultiFieldClassObjectWithWhitespace() {
        MultiFieldClass result = JSON.toObject(MultiFieldClass.class,
                " { \"name\" : \"123\" , \"type\" : \"1\" } ");

        assertNotNull(result);
        assertEquals("123", result.getName());
        assertEquals("1", result.getType());
    }

    @Test
    public void shouldParseContainerClassObject() {
        ContainerClass result = JSON.toObject(ContainerClass.class,
                "{\"name\":\"nameValue\",\"embedded\":{\"type\":\"typeValue\"}}");

        assertNotNull(result);
        assertEquals("nameValue", result.getName());
        assertNotNull(result.getEmbedded());
        assertEquals("typeValue", result.getEmbedded().getType());
    }

    @Test
    public void shouldParseContainerClassObjectWithWhitespace() {
        ContainerClass result = JSON.toObject(ContainerClass.class,
                " { \"name\" : \"nameValue\" , \"embedded\" : { \"type\" : \"typeValue\" } } ");

        assertNotNull(result);
        assertEquals("nameValue", result.getName());
        assertNotNull(result.getEmbedded());
        assertEquals("typeValue", result.getEmbedded().getType());
    }

    @Test
    public void shouldParseArrayContainerClass() {
        ArrayContainerClass result = JSON.toObject(ArrayContainerClass.class,
                "{\"name\":\"nameValue\",\"embeddedArray\":[{\"type\":\"typeValue1\"},{\"type\":\"typeValue2\"}]}");

        assertNotNull(result);
        assertEquals("nameValue", result.getName());
        assertNotNull(result.getEmbeddedArray());
        assertEquals(2, result.getEmbeddedArray().length);
        assertEquals("typeValue1", result.getEmbeddedArray()[0].getType());
        assertEquals("typeValue2", result.getEmbeddedArray()[1].getType());
    }

    @Test
    public void shouldParseArrayContainerClassWithWhitespace() {
        ArrayContainerClass result = JSON.toObject(ArrayContainerClass.class,
                " { \"name\" : \"nameValue\" , \"embeddedArray\" : [ { \"type\" : \"typeValue1\" } , { \"type\" : \"typeValue2\" } ] } ");

        assertNotNull(result);
        assertEquals("nameValue", result.getName());
        assertNotNull(result.getEmbeddedArray());
        assertEquals(2, result.getEmbeddedArray().length);
        assertEquals("typeValue1", result.getEmbeddedArray()[0].getType());
        assertEquals("typeValue2", result.getEmbeddedArray()[1].getType());
    }

    @Test
    public void shouldParse_i_number_double_huge_neg_exp() {
        double[] result = JSON.toDoubleArray("[123.456e-789]");

        assertEquals(1, result.length);
        BigDecimal value = new BigDecimal("123.456e-789");

        assertEquals(value.doubleValue(), result[0], 0.0D);
    }

    @Test(expected = ArithmeticException.class)
    public void shouldParse_i_number_huge_exp() {
        double[] result = JSON.toDoubleArray("[0.4e00669999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999969999999006]");

        assertEquals(1, result.length);
        BigDecimal value = new BigDecimal("0.4e00669999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999969999999006");

        assertEquals(value.doubleValue(), result[0], 0.0D);
    }

    @Test
    public void shouldParse_i_number_neg_int_huge_exp() {
        double[] result = JSON.toDoubleArray("[-1e+9999]");

        assertEquals(1, result.length);
        BigDecimal value = new BigDecimal("-1e+9999");

        assertEquals(value.doubleValue(), result[0], 0.0D);
    }

    @Test
    public void shouldParse_i_number_real_neg_overflow() {
        double[] result = JSON.toDoubleArray("[-123123e100000]");

        assertEquals(1, result.length);
        BigDecimal value = new BigDecimal("-123123e100000");

        assertEquals(value.doubleValue(), result[0], 0.0D);
    }

    @Test
    public void shouldParse_i_number_real_pos_overflow() {
        double[] result = JSON.toDoubleArray("[123123e100000]");

        assertEquals(1, result.length);
        BigDecimal value = new BigDecimal("123123e100000");

        assertEquals(value.doubleValue(), result[0], 0.0D);
    }

    @Test
    public void shouldParse_i_number_real_underflow() {
        double[] result = JSON.toDoubleArray("[123e-10000000]");

        assertEquals(1, result.length);
        BigDecimal value = new BigDecimal("123e-10000000");

        assertEquals(value.doubleValue(), result[0], 0.0D);
    }

    @Test
    public void shouldParse_i_number_too_big_neg_int() {
        long[] result = JSON.toLongArray("[-123123123123123123123123123123]");

        assertEquals(1, result.length);
        BigInteger bigInteger = new BigInteger("-123123123123123123123123123123");

        assertEquals(bigInteger.longValue(), result[0]);
    }

    @Test
    public void shouldParse_i_number_too_big_pos_int() {
        long[] result = JSON.toLongArray("[100000000000000000000]");

        assertEquals(1, result.length);
        BigInteger bigInteger = new BigInteger("100000000000000000000");

        assertEquals(bigInteger.longValue(), result[0]);
    }

    @Test
    public void should_i_number_very_big_negative_int() {
        long[] result = JSON.toLongArray("[-237462374673276894279832749832423479823246327846]");

        assertEquals(1, result.length);
        BigInteger bigInteger = new BigInteger("-237462374673276894279832749832423479823246327846");

        assertEquals(bigInteger.longValue(), result[0]);
    }

    @Test
    public void should_i_object_key_lone_2nd_surrogate() {
        Map<String, Object> map = JSON.toObject("{\"\\uDFAA\":0}");

        assertEquals(0, ((Long) map.get("\uDFAA")).intValue());
    }

    @Test
    public void should_i_string_1st_surrogate_but_2nd_missing() {
        String[] result = JSON.toStringArray("[\"\uDADA\"]");

        assertEquals(1, result.length);

        assertEquals("\uDADA", result[0]);
    }

    @Test
    public void should_i_string_1st_valid_surrogate_2nd_invalid() {
        String[] result = JSON.toStringArray("[\"\uD888\u1234\"]");

        assertEquals(1, result.length);

        assertEquals("\uD888\u1234", result[0]);
    }
}
