package com.polyglotsoft.json;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class JSON {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JSON() {
        // No Instantiations
    }

    private static final Set<Class<?>> WRAPPER_TYPES = new HashSet<>(Arrays.asList(
            Boolean.class,
            Character.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class
    ));

    private static boolean isWrapper(Class<?> type) {
        return WRAPPER_TYPES.contains(type);
    }

    private static boolean isSingleByteUnicodeControlCharacter(char c) {
        return c <= '\u001F' || (c >= '\u007F' && c <= '\u009F');
    }

    private static boolean isDoubleByteUnicodeControlCharacter(char c) {
        return '\u2000' <= c && c <= '\u20FF';
    }

    private static void writeString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); ++i) {
            char ch = value.charAt(i);

            if ('\b' == ch) {
                sb.append("\\b");
            } else if ('\f' == ch) {
                sb.append("\\f");
            } else if ('\n' == ch) {
                sb.append("\\n");
            } else if ('\r' == ch) {
                sb.append("\\r");
            } else if ('\t' == ch) {
                sb.append("\\t");
            } else if ('"' == ch) {
                sb.append("\\\"");
            } else if ('/' == ch) {
                sb.append("\\/");
            } else if ('\\' == ch) {
                sb.append("\\\\");
            } else if (isSingleByteUnicodeControlCharacter(ch)) {
                sb.append("\\u00");

                sb.append(Integer.toHexString(ch).toUpperCase(Locale.ROOT));
            } else if (isDoubleByteUnicodeControlCharacter(ch)) {
                sb.append("\\u");

                sb.append(Integer.toHexString(ch).toUpperCase(Locale.ROOT));
            } else {
                sb.append(ch);
            }
        }
        sb.append('"');
    }

    private static void writeArray(StringBuilder sb, Object value)
            throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        sb.append('[');
        int arrayLength = Array.getLength(value);
        for (int elementIndex = 0; elementIndex < arrayLength; ++elementIndex) {
            writeAny(sb, Array.get(value, elementIndex));

            if (elementIndex + 1 < arrayLength) {
                sb.append(',');
            }
        }
        sb.append(']');
    }

    private static void writeIterable(StringBuilder sb, Iterable<?> value)
            throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        sb.append('[');
        for (Object element : value) {
            writeAny(sb, element);

            sb.append(',');
        }

        if (',' == sb.charAt(sb.length() - 1)) {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append(']');
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map)
            throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        sb.append('{');

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();

            if (null == value) {
                continue;
            }

            sb.append('"');
            sb.append(entry.getKey());
            sb.append("\":");

            writeAny(sb, value);

            sb.append(',');
        }

        if (',' == sb.charAt(sb.length() - 1)) {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append('}');
    }

    private static void writeObject(StringBuilder sb, Object object)
            throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        sb.append('{');

        BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

        for (int propertyIndex = 0; propertyIndex < propertyDescriptors.length; ++propertyIndex) {
            PropertyDescriptor propertyDescriptor = propertyDescriptors[propertyIndex];

            if ("class".equals(propertyDescriptor.getName())) {
                continue;
            }

            Method readMethod = propertyDescriptor.getReadMethod();

            if (null == readMethod) {
                continue;
            }

            Object value = readMethod.invoke(object);

            if (null == value) {
                continue;
            }

            sb.append('"');
            sb.append(propertyDescriptor.getName());
            sb.append("\":");

            writeAny(sb, value);

            sb.append(',');
        }

        if (',' == sb.charAt(sb.length() - 1)) {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append('}');
    }

    private static void writeAny(StringBuilder sb, Object object)
            throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        if (null == object) {
            sb.append("null");
        } else {
            Class<?> type = object.getClass();

            if (type.isPrimitive()) {
                sb.append(object);
            } else if (String.class.isAssignableFrom(type)) {
                writeString(sb, (String) object);
            } else if (isWrapper(type)) {
                sb.append(object);
            } else if (type.isArray()) {
                writeArray(sb, object);
            } else if (Iterable.class.isAssignableFrom(type)) {
                writeIterable(sb, (Iterable<?>) object);
            } else if (Map.class.isAssignableFrom(type)) {
                writeMap(sb, (Map<?, ?>) object);
            } else if (LocalDateTime.class.isAssignableFrom(type)) {
                writeString(sb, ((LocalDateTime) object).format(dateTimeFormatter));
            } else if (InetAddress.class.isAssignableFrom(type)) {
                writeString(sb, ((InetAddress) object).getHostAddress());
            } else if (Enum.class.isAssignableFrom(type)) {
                writeString(sb, ((Enum<?>) object).name());
            } else if (AtomicInteger.class.isAssignableFrom(type)) {
                sb.append(((AtomicInteger) object).get());
            } else if (AtomicLong.class.isAssignableFrom(type)) {
                sb.append(((AtomicLong) object).get());
            } else if (AtomicReference.class.isAssignableFrom(type)) {
                writeAny(sb, ((AtomicReference<?>) object).get());
            } else if (BigInteger.class.isAssignableFrom(type)) {
                sb.append(((BigInteger) object).toString(10));
            } else {
                writeObject(sb, object);
            }
        }
    }

    private static String readInputStream(InputStream inputStream) {
        if (null == inputStream) {
            return null;
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (true) {
                int read = inputStream.read(buffer);

                if (-1 == read) {
                    break;
                }

                bos.write(buffer, 0, read);
            }

            return bos.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class Parser {
        private static final Object EMPTY_ARRAY = new Object[0];

        private static final Map<String, Object> EMPTY_OBJECT = Collections.emptyMap();

        private final char[] chars;

        private final Map<Class<?>, Map<String, Method>> writeMethodMapCache = new HashMap<>();

        private final LinkedList<Object> valueStack = new LinkedList<>();

        public Parser(char[] chars) {
            this.chars = chars;
        }

        private <T> T createInstanceOf(Class<T> type) {
            Map<String, Method> writeMethodMap = writeMethodMapCache.get(type);

            if (null == writeMethodMap) {
                writeMethodMap = new TreeMap<>();

                try {
                    BeanInfo beanInfo = Introspector.getBeanInfo(type);

                    for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                        if ("class".equals(propertyDescriptor.getName())) {
                            continue;
                        }

                        writeMethodMap.put(propertyDescriptor.getName(), propertyDescriptor.getWriteMethod());
                    }

                    writeMethodMapCache.put(type, writeMethodMap);
                } catch (IntrospectionException e) {
                    throw new IllegalStateException(e);
                }
            }

            try {
                return type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * ""
         * '0020' ws
         * '000A' ws
         * '000D' ws
         * '0009' ws
         */
        private int nextWhiteSpace(int startIndex) {
            int index = startIndex;
            while (0x0020 == chars[index] || 0x000A == chars[index] || 0x000D == chars[index] || 0x0009 == chars[index]) {
                ++index;
            }

            return index;
        }

        /**
         * true
         */
        private int nextTrue(int startIndex) {
            if (0 < (chars.length - startIndex - 3)
                    && 't' == chars[startIndex]
                    && 'r' == chars[startIndex + 1]
                    && 'u' == chars[startIndex + 2]
                    && 'e' == chars[startIndex + 3]) {
                valueStack.push(true);

                return startIndex + 4;
            }

            return startIndex;
        }

        /**
         * false
         */
        private int nextFalse(int startIndex) {
            if (0 < (chars.length - startIndex - 4)
                    && 'f' == chars[startIndex]
                    && 'a' == chars[startIndex + 1]
                    && 'l' == chars[startIndex + 2]
                    && 's' == chars[startIndex + 3]
                    && 'e' == chars[startIndex + 4]) {
                valueStack.push(false);

                return startIndex + 5;
            }

            return startIndex;
        }

        /**
         * null
         */
        private int nextNull(int startIndex) {
            if (0 < (chars.length - startIndex - 3)
                    && 'n' == chars[startIndex]
                    && 'u' == chars[startIndex + 1]
                    && 'l' == chars[startIndex + 2]
                    && 'l' == chars[startIndex + 3]) {
                valueStack.push(null);

                return startIndex + 4;
            }

            return startIndex;
        }

        /**
         * digit
         * 'A' . 'F'
         * 'a' . 'f'
         */
        private int nextHex(int startIndex) {
            if (0 < chars.length - startIndex) {
                int index = nextDigit(startIndex);

                if (index == startIndex
                        && (('A' <= chars[startIndex] && chars[startIndex] <= 'F')
                        || ('a' <= chars[startIndex] && chars[startIndex] <= 'f'))) {
                    return startIndex + 1;
                }

                return index;
            }

            return startIndex;
        }

        /**
         * '0020' . '10FFFF' - '"' - '\'
         * '\' escape
         */
        private int nextCharacter(int startIndex) {
            if (0 < chars.length - startIndex) {
                if (0x0020 <= chars[startIndex] && '"' != chars[startIndex] && '\\' != chars[startIndex]) {
                    return startIndex + 1;
                }

                if ('\\' == chars[startIndex]) {
                    return nextEscape(startIndex + 1);
                }
            }

            return startIndex;
        }

        /**
         * ""
         * character characters
         */
        private int nextCharacters(int startIndex) {
            if (0 < chars.length - startIndex) {
                StringBuilder sb = (StringBuilder) valueStack.pop();

                int index = nextCharacter(startIndex);
                if (1 < index - startIndex) {
                    sb.append(valueStack.pop());

                    valueStack.push(sb);

                    return nextCharacters(index);
                }

                if (index != startIndex) {
                    sb.append(chars[index - 1]);

                    valueStack.push(sb);

                    return nextCharacters(index);
                }
            }

            return startIndex;
        }

        /**
         * '"' characters '"'
         */
        private int nextString(int startIndex) {
            if (0 < chars.length - startIndex && '"' == chars[startIndex]) {
                StringBuilder sb = new StringBuilder();

                valueStack.push(sb);

                int index = nextCharacters(startIndex + 1);

                if (index != startIndex && 0 < chars.length - index && '"' == chars[index]) {
                    valueStack.push(sb.toString());

                    return index + 1;
                }
            }

            return startIndex;
        }

        private int toUnsignedByte(char hexChar1, char hexChar2) {
            int unsignedByte;
            if ('A' <= hexChar1 && hexChar1 <= 'F') {
                unsignedByte = (hexChar1 - 'A' + 10) * 16;
            } else {
                unsignedByte = (hexChar1 - '0') * 16;
            }

            if ('A' <= hexChar2 && hexChar2 <= 'F') {
                unsignedByte += hexChar2 - 'A' + 10;
            } else {
                unsignedByte += hexChar2 - '0';
            }

            return unsignedByte;
        }


        /**
         * '"'
         * '\'
         * '/'
         * 'b'
         * 'f'
         * 'n'
         * 'r'
         * 't'
         * 'u' hex hex hex hex
         */
        private int nextEscape(int startIndex) {
            if (0 < chars.length - startIndex) {
                if ('"' == chars[startIndex] || '\\' == chars[startIndex] || '/' == chars[startIndex]
                        || 'b' == chars[startIndex] || 'f' == chars[startIndex] || 'n' == chars[startIndex]
                        || 'r' == chars[startIndex] || 't' == chars[startIndex]) {
                    valueStack.push(chars[startIndex]);

                    return startIndex + 1;
                }

                if ('u' == chars[startIndex] && 0 < chars.length - startIndex - 4) {
                    int index = nextHex(startIndex + 1);
                    index = nextHex(index);
                    index = nextHex(index);
                    index = nextHex(index);

                    if (index == startIndex + 5) {
                        valueStack.push("" + Character.lowSurrogate(Integer.parseInt(""
                                + chars[startIndex + 1] + chars[startIndex + 2]
                                + chars[startIndex + 3] + chars[startIndex + 4], 16)));
                    }

                    return index;
                }
            }

            return startIndex;
        }

        /**
         * '1' . '9'
         */
        private int nextOneNine(int startIndex) {
            if (0 < chars.length - startIndex && '1' <= chars[startIndex] && chars[startIndex] <= '9') {
                return startIndex + 1;
            }

            return startIndex;
        }

        /**
         * '0'
         * onenine
         */
        private int nextDigit(int startIndex) {
            if (0 < chars.length - startIndex) {
                if ('0' == chars[startIndex]) {
                    return startIndex + 1;
                }

                return nextOneNine(startIndex);
            }

            return startIndex;
        }

        /**
         * digit
         * digit digits
         */
        private int nextDigits(int startIndex) {
            int index = nextDigit(startIndex);

            if (index == startIndex) {
                return startIndex;
            }

            return nextDigits(index);
        }

        /**
         * digit
         * onenine digits
         * '-' digit
         * '-' onenine digits
         */
        private int nextInteger(int startIndex) {
            int index = startIndex;

            if (0 < chars.length - index) {
                if ('-' == chars[index]) {
                    index = nextDigit(index + 1);

                    if (index - 2 == startIndex) {
                        index = nextOneNine(index);
                        index = nextDigits(index);
                    }
                } else {
                    index = nextDigit(index);

                    if (index - 1 == startIndex) {
                        index = nextOneNine(index);
                        index = nextDigits(index);
                    }
                }
            }

            return index;
        }

        /**
         * ""
         * '.' digits
         */
        private int nextFraction(int startIndex) {
            if (0 < chars.length - startIndex && '.' == chars[startIndex]) {
                return nextDigits(startIndex + 1);
            }

            return startIndex;
        }

        /**
         * ""
         * '+'
         * '-'
         */
        private int nextSign(int startIndex) {
            if (0 < chars.length - startIndex && '+' == chars[startIndex] || '-' == chars[startIndex]) {
                return startIndex + 1;
            }

            return startIndex;
        }

        /**
         * ""
         * 'E' sign digits
         * 'e' sign digits
         */
        private int nextExponent(int startIndex) {
            if (0 < chars.length - startIndex && ('E' == chars[startIndex] || 'e' == chars[startIndex])) {
                int index = startIndex + 1;

                index = nextSign(index);
                index = nextDigits(index);

                return index;
            }

            return startIndex;
        }

        /**
         * integer fraction exponent
         */
        private int nextNumber(int startIndex) {
            int index = startIndex;

            index = nextInteger(index);

            boolean hasInteger = index != startIndex;

            int previousIndex = index;
            int integerLastIndex = index;

            index = nextFraction(index);

            boolean hasFraction = index != previousIndex;

            int fractionStartIndex = previousIndex;
            int fractionLastIndex = index;
            previousIndex = index;

            index = nextExponent(index);

            boolean hasExponent = index != previousIndex;

            int exponentStartIndex = previousIndex;
            int exponentLastIndex = index;

            long longValue = 0L;
            if (hasInteger) {
                int start;
                if ('-' == chars[startIndex]) {
                    start = startIndex + 1;
                } else {
                    start = startIndex;
                }

                long digitValue = 1L;
                long digitsValue = 0L;
                for (int i = integerLastIndex - 1; i != start - 1; --i) {
                    digitsValue += (chars[i] - '0') * digitValue;

                    digitValue *= 10;
                }

                if ('-' == chars[startIndex]) {
                    digitsValue = -digitsValue;
                }

                longValue = digitsValue;
            }

            double doubleValue = longValue;
            if (hasFraction) {
                long digitValue = 1L;
                long decimalPart = 0L;
                for (int i = fractionLastIndex - 1; i != fractionStartIndex; --i) {
                    decimalPart += (chars[i] - '0') * digitValue;

                    digitValue *= 10;
                }

                doubleValue += ((double) decimalPart) / digitValue;
            }

            if (hasExponent) {
                int digitsStartIndex = exponentStartIndex + 1;
                boolean negate = false;
                if ('-' == chars[exponentStartIndex + 1]) {
                    digitsStartIndex = exponentStartIndex + 2;
                    negate = true;
                } else {
                    if ('+' == chars[exponentStartIndex + 1]) {
                        digitsStartIndex = exponentStartIndex + 2;
                    }
                }

                BigDecimal bigDecimal = BigDecimal.valueOf(doubleValue);

                int digitValue = 1;
                int exponentPart = 0;
                for (int i = exponentLastIndex - 1; i != digitsStartIndex - 1; --i) {
                    exponentPart += (chars[i] - '0') * digitValue;

                    digitValue *= 10;

                    if (digitValue == 1000000000) {
                        digitValue = 1;

                        if (negate) {
                            bigDecimal = bigDecimal.movePointLeft(exponentPart);
                        } else {
                            bigDecimal = bigDecimal.movePointRight(exponentPart);
                        }

                        exponentPart = 0;
                    }
                }

                if (0 < exponentPart) {
                    if (negate) {
                        bigDecimal = bigDecimal.movePointLeft(exponentPart);
                    } else {
                        bigDecimal = bigDecimal.movePointRight(exponentPart);
                    }
                }

                doubleValue = bigDecimal.doubleValue();
            }

            if (index != startIndex) {
                if (hasFraction || hasExponent) {
                    valueStack.push(doubleValue);
                } else {
                    valueStack.push(longValue);
                }
            }

            return index;
        }

        /**
         * ws string ws ':' element
         */
        private int nextMember(int startIndex) {
            int index = nextWhiteSpace(startIndex);

            index = nextString(index);

            if (index != startIndex) {
                String memberKey = (String) valueStack.pop();

                index = nextWhiteSpace(index);

                if (0 < chars.length - index && ':' == chars[index]) {
                    int memberValueLastIndex = nextElement(index + 1);

                    Object memberValue = valueStack.pop();

                    valueStack.push(new Object[] { memberKey, memberValue });

                    return memberValueLastIndex;
                }
            }

            return index;
        }

        /**
         * member
         * member ',' members
         */
        private int nextMembers(int startIndex) {
            int index = nextMember(startIndex);

            if (index == startIndex) {
                return index;
            }

            Object[] member = (Object[]) valueStack.pop();
            Map<String, Object> members = (Map<String, Object>) valueStack.pop();

            members.put((String) member[0], member[1]);

            if (0 < chars.length - index && ',' == chars[index]) {
                valueStack.push(members);

                index = nextMembers(index + 1);
            }

            return index;
        }

        /**
         * object
         * array
         * string
         * number
         * "true"
         * "false"
         * "null"
         */
        private int nextValue(int startIndex) {
            int index = startIndex;

            index = nextObject(index);

            if (index == startIndex) {
                index = nextArray(index);
            }

            if (index == startIndex) {
                index = nextString(index);
            }

            if (index == startIndex) {
                index = nextNumber(index);
            }

            if (index == startIndex) {
                index = nextTrue(index);
            }

            if (index == startIndex) {
                index = nextFalse(index);
            }

            if (index == startIndex) {
                index = nextNull(index);
            }

            return index;
        }

        /**
         * ws value ws
         */
        private int nextElement(int startIndex) {
            int index = startIndex;

            index = nextWhiteSpace(index);
            index = nextValue(index);
            index = nextWhiteSpace(index);

            return index;
        }

        /**
         * element
         * element ',' elements
         */
        @SuppressWarnings("unchecked")
        private int nextElements(int startIndex) {
            int index = nextElement(startIndex);

            if (index == startIndex) {
                return index;
            }

            Object element = valueStack.pop();
            List<Object> elements = (List<Object>) valueStack.pop();

            elements.add(element);

            if (0 < chars.length - index && ',' == chars[index]) {
                valueStack.push(elements);

                return nextElements(index + 1);
            }

            return index;
        }

        /**
         * '[' ws ']'
         * '[' elements ']'
         */
        private int nextArray(int startIndex) {
            if (0 < chars.length - startIndex && '[' == chars[startIndex]) {
                int index = nextWhiteSpace(startIndex + 1);

                if (0 < chars.length - index && ']' == chars[index]) {
                    valueStack.push(EMPTY_ARRAY);

                    return index + 1;
                }

                List<Object> list = new ArrayList<>();

                valueStack.push(list);

                index = nextElements(startIndex + 1);

                if (index != startIndex + 1 && 0 < chars.length - index && ']' == chars[index]) {
                    valueStack.push(list.toArray(new Object[0]));

                    return index + 1;
                }
            }

            return startIndex;
        }

        /**
         * '{' ws '}'
         * '{' members '}'
         */
        private int nextObject(int startIndex) {
            if (0 < chars.length - startIndex && '{' == chars[startIndex]) {
                int index = nextWhiteSpace(startIndex + 1);

                if (0 < chars.length - index && '}' == chars[index]) {
                    valueStack.push(EMPTY_OBJECT);

                    return index + 1;
                }

                Map<String, Object> members = new HashMap<>();

                valueStack.push(members);

                index = nextMembers(startIndex + 1);

                if (index != startIndex + 1 && 0 < chars.length - index && '}' == chars[index]) {
                    valueStack.push(members);

                    return index + 1;
                }
            }

            return startIndex;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> parseObject() {
            int index = nextObject(0);

            if (0 != index) {
                return (Map<String, Object>) valueStack.pop();
            }

            throw new NoSuchElementException("no object is available for parsing");
        }

        @SuppressWarnings("unchecked")
        public <T> T parseObject(Class<T> type) {
            T instance = createInstanceOf(type);

            int index = nextObject(0);

            if (0 != index) {
                populateInstance(instance, (Map<String, Object>) valueStack.pop());

                return instance;
            }

            throw new NoSuchElementException("no object is available for parsing");
        }

        private Object populateInstance(Object instance, Map<String, Object> members) {
            Map<String, Method> methodMap = writeMethodMapCache.get(instance.getClass());

            try {
                for (Map.Entry<String, Method> methodEntry : methodMap.entrySet()) {
                    Object value = members.get(methodEntry.getKey());

                    if (null == value) {
                        continue;
                    }

                    if (value instanceof Map) {
                        Object embeddedInstance = createInstanceOf(methodEntry.getValue().getParameterTypes()[0]);

                        methodEntry.getValue().invoke(instance,
                                populateInstance(embeddedInstance, (Map<String, Object>) value));
                    } else if (value instanceof Long) {
                        Class<?> parameterType = methodEntry.getValue().getParameterTypes()[0];

                        if (short.class.equals(parameterType) || Short.class.equals(parameterType)) {
                            methodEntry.getValue().invoke(instance, ((Long) value).shortValue());
                        } else if (int.class.equals(parameterType) || Integer.class.equals(parameterType)) {
                            methodEntry.getValue().invoke(instance, ((Long) value).intValue());
                        } else if (float.class.equals(parameterType) || Float.class.equals(parameterType)) {
                            methodEntry.getValue().invoke(instance, ((Long) value).floatValue());
                        } else if (double.class.equals(parameterType) || Double.class.equals(parameterType)) {
                            methodEntry.getValue().invoke(instance, ((Long) value).doubleValue());
                        } else if (AtomicInteger.class.equals(parameterType)) {
                            methodEntry.getValue().invoke(instance, new AtomicInteger(((Long) value).intValue()));
                        } else if (AtomicLong.class.equals(parameterType)) {
                            methodEntry.getValue().invoke(instance, new AtomicLong((Long) value));
                        } else {
                            methodEntry.getValue().invoke(instance, value);
                        }
                    } else if (value instanceof Double) {
                        if (float.class.equals(methodEntry.getValue().getParameterTypes()[0])
                                || Float.class.equals(methodEntry.getValue().getParameterTypes()[0])) {
                            methodEntry.getValue().invoke(instance, ((Double) value).floatValue());
                        } else {
                            methodEntry.getValue().invoke(instance, value);
                        }
                    } else if (value instanceof String) {
                        if (LocalDateTime.class.equals(methodEntry.getValue().getParameterTypes()[0])) {
                            methodEntry.getValue().invoke(instance,
                                    LocalDateTime.parse((String) value, dateTimeFormatter));
                        } else {
                            methodEntry.getValue().invoke(instance, value);
                        }
                    } else if (value instanceof Object[]) {
                        Object[] objectArray = (Object[]) value;

                        if (methodEntry.getValue().getParameterTypes()[0].isArray()) {
                            Object valueArray = Array.newInstance(methodEntry.getValue().getParameterTypes()[0]
                                            .getComponentType(), objectArray.length);

                            for (int i = 0; i < objectArray.length; ++i) {
                                Object member = createInstanceOf(methodEntry.getValue().getParameterTypes()[0]
                                        .getComponentType());

                                populateInstance(member, (Map<String, Object>) objectArray[i]);

                                Array.set(valueArray, i, member);
                            }

                            methodEntry.getValue().invoke(instance, valueArray);
                        } else {
                            throw new IllegalStateException("Unexpected array");
                        }
                    } else {
                        methodEntry.getValue().invoke(instance, value);
                    }
                }

                return instance;
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public boolean parseBoolean() {
            int index = nextTrue(0);

            return 0 != index;
        }

        public byte parseByte() {
            int index = nextNumber(0);

            long longValue = (long) valueStack.pop();

            if (0 != index) {
                if (Byte.MAX_VALUE < longValue || longValue < Byte.MIN_VALUE) {
                    throw new ArithmeticException("parsed numeric value cannot be represented as a byte");
                }

                return (byte) longValue;
            }

            throw new NoSuchElementException("no number is available for parsing");
        }

        public short parseShort() {
            int index = nextNumber(0);

            long longValue = (long) valueStack.pop();

            if (0 != index) {
                if (Short.MAX_VALUE < longValue || Short.MIN_VALUE > longValue) {
                    throw new ArithmeticException("parsed numeric value cannot be represented as a short");
                }

                return (short) longValue;
            }

            throw new NoSuchElementException("no number is available for parsing");
        }

        public int parseInt() {
            if (0 != nextNumber(0)) {
                long longValue = (long) valueStack.pop();

                if (Integer.MAX_VALUE < longValue || Integer.MIN_VALUE > longValue) {
                    throw new ArithmeticException("parsed numeric value cannot be represented as a integer");
                }

                return (int) longValue;
            }

            throw new NoSuchElementException("no number is available for parsing");
        }

        public long parseLong() {
            if (0 != nextNumber(0)) {
                return (long) valueStack.pop();
            }

            throw new NoSuchElementException("no number is available for parsing");
        }

        public float parseFloat() {
            if (0 != nextNumber(0)) {
                return (float) ((Double) valueStack.pop()).doubleValue();
            }

            throw new NoSuchElementException("no number is available for parsing");
        }

        public double parseDouble() {
            if (0 != nextNumber(0)) {
                return (double) valueStack.pop();
            }

            throw new NoSuchElementException("no number is available for parsing");
        }

        public String parseString() {
            if (0 != nextString(0)) {
                return (String) valueStack.pop();
            }

            throw new NoSuchElementException("no string is available for parsing");
        }

        public Object parseArray() {
            if (0 != nextArray(0)) {
                return valueStack.pop();
            }

            throw new NoSuchElementException("no array is available for parsing");
        }
    }

    public static String toJSON(Object object) {
        StringBuilder sb = new StringBuilder();

        try {
            writeAny(sb, object);

            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean toBoolean(String json) {
        return new Parser(json.trim().toCharArray()).parseBoolean();
    }

    public static byte toByte(String json) {
        return new Parser(json.trim().toCharArray()).parseByte();
    }

    public static short toShort(String json) {
        return new Parser(json.trim().toCharArray()).parseShort();
    }

    public static int toInt(String json) {
        return new Parser(json.trim().toCharArray()).parseInt();
    }

    public static long toLong(String json) {
        return new Parser(json.trim().toCharArray()).parseLong();
    }

    public static float toFloat(String json) {
        return new Parser(json.trim().toCharArray()).parseFloat();
    }

    public static double toDouble(String json) {
        return new Parser(json.trim().toCharArray()).parseDouble();
    }

    public static String toString(String json) {
        return new Parser(json.trim().toCharArray()).parseString();
    }

    public static boolean[] toBooleanArray(String json) {
        Object array = new Parser(json.trim().toCharArray()).parseArray();

        boolean[] result = (boolean[]) Array.newInstance(boolean.class, Array.getLength(array));

        for (int i = 0; i < result.length; ++i) {
            result[i] = (boolean) Array.get(array, i);
        }

        return result;
    }

    public static byte[] toByteArray(String json) {
        Object array = new Parser(json.trim().toCharArray()).parseArray();

        byte[] result = (byte[]) Array.newInstance(byte.class, Array.getLength(array));

        for (int i = 0; i < result.length; ++i) {
            result[i] = (byte) ((Long) Array.get(array, i)).longValue();
        }

        return result;
    }

    public static short[] toShortArray(String json) {
        Object array = new Parser(json.trim().toCharArray()).parseArray();

        short[] result = (short[]) Array.newInstance(short.class, Array.getLength(array));

        for (int i = 0; i < result.length; ++i) {
            result[i] = (short) ((Long) Array.get(array, i)).longValue();
        }

        return result;
    }

    public static int[] toIntArray(String json) {
        Object array = new Parser(json.trim().toCharArray()).parseArray();

        int[] result = (int[]) Array.newInstance(int.class, Array.getLength(array));

        for (int i = 0; i < result.length; ++i) {
            result[i] = (int) ((Long) Array.get(array, i)).longValue();
        }

        return result;
    }

    public static long[] toLongArray(String json) {
        Object array = new Parser(json.trim().toCharArray()).parseArray();

        long[] result = (long[]) Array.newInstance(long.class, Array.getLength(array));

        for (int i = 0; i < result.length; ++i) {
            result[i] = (Long) Array.get(array, i);
        }

        return result;
    }

    public static float[] toFloatArray(String json) {
        Object array = new Parser(json.trim().toCharArray()).parseArray();

        float[] result = (float[]) Array.newInstance(float.class, Array.getLength(array));

        for (int i = 0; i < result.length; ++i) {
            result[i] = (float) ((Double) Array.get(array, i)).doubleValue();
        }

        return result;
    }

    public static double[] toDoubleArray(String json) {
        Object array = new Parser(json.trim().toCharArray()).parseArray();

        double[] result = (double[]) Array.newInstance(double.class, Array.getLength(array));

        for (int i = 0; i < result.length; ++i) {
            result[i] = (Double) Array.get(array, i);
        }

        return result;
    }

    public static String[] toStringArray(String json) {
        Object array = new Parser(json.trim().toCharArray()).parseArray();

        String[] result = (String[]) Array.newInstance(String.class, Array.getLength(array));

        for (int i = 0; i < result.length; ++i) {
            result[i] = (String) Array.get(array, i);
        }

        return result;
    }

    public static List<Map<String, Object>> toObjectArray(String json) {
        Object result = new Parser(json.trim().toCharArray()).parseArray();

        if (result == Parser.EMPTY_ARRAY) {
            return Collections.emptyList();
        }

        return (List<Map<String, Object>>) result;
    }

    public static <T> T[] toObjectArray(Class<T> type, String json) {
        Parser parser = new Parser(json.trim().toCharArray());

        Object[] objectArray = (Object[]) parser.parseArray();

        T[] result = (T[]) Array.newInstance(type, Array.getLength(objectArray));

        for (int i = 0; i < objectArray.length; ++i) {
            Map<String, Object> object = (Map<String, Object>) Array.get(objectArray, i);

            T instance = parser.createInstanceOf(type);

            result[i] = type.cast(parser.populateInstance(instance, object));
        }

        return result;
    }

    public static <T> T[] toObjectArray(Class<T> type, InputStream inputStream) {
        return toObjectArray(type, readInputStream(inputStream));
    }

    public static <T> T toObject(Class<T> type, String json) {
        if (null == json) {
            return null;
        }

        return new Parser(json.trim().toCharArray()).parseObject(type);
    }

    public static <T> T toObject(Class<T> type, InputStream inputStream) {
        return toObject(type, readInputStream(inputStream));
    }

    public static Map<String, Object> toObject(String json) {
        if (null == json) {
            return null;
        }

        return new Parser(json.trim().toCharArray()).parseObject();
    }
}
