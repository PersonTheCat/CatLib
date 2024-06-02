package personthecat.catlib.config;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("BooleanMethodIsAlwaysInverted") // guard pattern
public final class ConfigUtil {
    private static final List<Class<?>> PRIMITIVES = List.of(
        String.class, Byte.class, byte.class, Short.class, int.class, Integer.class, int.class, Long.class, long.class,
        Float.class, float.class, Double.class, double.class, Boolean.class, boolean.class, Collection.class, Map.class);

    private ConfigUtil() {}

    public static Class<?> toBoxedType(Class<?> c) {
        if (c == boolean.class) return Boolean.class;
        if (c == char.class) return Character.class;
        if (c == byte.class) return Byte.class;
        if (c == short.class) return Short.class;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        return c;
    }

    public static Class<?> widen(Class<?> c) {
        return Number.class.isAssignableFrom(c) ? Number.class : c;
    }

    public static boolean isLiteralValue(Class<?> clazz) {
        if (clazz.isArray() || clazz.isEnum()) {
            return true;
        }
        for (final Class<?> primitive : PRIMITIVES) {
            if (primitive.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBoolean(Class<?> c) {
        return Boolean.class.isAssignableFrom(c) || boolean.class.isAssignableFrom(c);
    }

    public static boolean isInteger(Class<?> c) {
        return Byte.class.isAssignableFrom(c) || byte.class.isAssignableFrom(c)
            || Short.class.isAssignableFrom(c) || short.class.isAssignableFrom(c)
            || Integer.class.isAssignableFrom(c) || int.class.isAssignableFrom(c);
    }

    public static boolean isLong(Class<?> c) {
        return Long.class.isAssignableFrom(c) || long.class.isAssignableFrom(c);
    }

    public static boolean isFloat(Class<?> c) {
        return Float.class.isAssignableFrom(c) || float.class.isAssignableFrom(c);
    }

    public static boolean isDouble(Class<?> c) {
        return Double.class.isAssignableFrom(c) || double.class.isAssignableFrom(c);
    }

    @ExpectPlatform
    public static Object remap(Class<?> t, Class<?>[] generics, Object o) {
        return o;
    }

    public static Object toCorrectPrimitive(Class<?> t, Object o) {
        if (!(o instanceof Number n)) {
            return o;
        } else if (Byte.class.isAssignableFrom(t) || byte.class.isAssignableFrom(t)) {
            return n.byteValue();
        } else if (Short.class.isAssignableFrom(t) || short.class.isAssignableFrom(t)) {
            return n.shortValue();
        } else if (Integer.class.isAssignableFrom(t) || int.class.isAssignableFrom(t)) {
            return n.intValue();
        } else if (Long.class.isAssignableFrom(t) || long.class.isAssignableFrom(t)) {
            return n.longValue();
        } else if (Float.class.isAssignableFrom(t) || float.class.isAssignableFrom(t)) {
            return n.floatValue();
        } else if (Double.class.isAssignableFrom(t) || double.class.isAssignableFrom(t)) {
            return n.doubleValue();
        }
        return o;
    }

    public static List<Object> arrayToList(Object array) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < Array.getLength(array); i++) {
            list.add(Array.get(array, i));
        }
        return list;
    }

    public static Object listToArray(List<?> list, Class<?> type) {
        final Object array = Array.newInstance(type, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, list.get(i));
        }
        return array;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Object> collectionToList(Object def) {
        if (def instanceof List l) return l;
        else if (def instanceof Collection c) return new ArrayList<>(c);
        return new ArrayList<>(List.of(def));
    }

    public static Class<?>[] shiftGenerics(Class<?>[] generics) {
        if (generics.length == 0) return generics;
        final Class<?>[] shifted = new Class<?>[generics.length - 1];
        System.arraycopy(generics, 1, shifted, 0, generics.length - 1);
        return shifted;
    }

    public static Class<?>[] getGenericTypes(String filename, ConfigValue value) throws ValueException {
        if (!Map.class.isAssignableFrom(value.type()) && !Collection.class.isAssignableFrom(value.type())) {
            return new Class[0];
        }
        final List<Class<?>> list = new ArrayList<>();
        if (value instanceof FieldValue f) {
            addGenericsFromType(list, filename, value, f.getField().getGenericType());
        } else {
            addGenericsFromValue(list, filename, value, value.defaultValue());
        }
        return list.toArray(new Class<?>[0]);
    }

    private static void addGenericsFromType(
            List<Class<?>> list, String filename, ConfigValue value, Type t) throws ValueException {
        if (!(t instanceof ParameterizedType p)) {
            return;
        }
        if (isAssignable(Map.class, p.getRawType())) {
            final Type[] actual = p.getActualTypeArguments();
            expectParameterCount(actual, 2, filename, value);
            if (!isAssignable(String.class, actual[0])) {
                final String msg = "Map values must have string keys";
                throw new ValueException(msg, filename, value);
            }
            addTypeParameters(list, filename, value, actual[1]);
        } else if (isAssignable(Collection.class, p.getRawType())) {
            final Type[] actual = p.getActualTypeArguments();
            expectParameterCount(actual, 1, filename, value);
            addTypeParameters(list, filename, value, actual[0]);
        }
    }

    private static boolean isAssignable(Class<?> to, Type from) {
        return from instanceof Class<?> c && to.isAssignableFrom(c);
    }

    private static void expectParameterCount(Type[] actual, int count, String filename, ConfigValue value) throws ValueException {
        if (actual.length != count) {
            final String msg = "Cannot resolve generic parameters from field: " + value;
            throw new ValueException(msg, filename, value);
        }
    }

    private static void addTypeParameters(List<Class<?>> list, String filename, ConfigValue value, Type type) throws ValueException {
        if (type instanceof ParameterizedType p && p.getRawType() instanceof Class c) {
            list.add(c);
        }
        addGenericsFromType(list, filename, value, type);
    }

    private static void addGenericsFromValue(
            List<Class<?>> list, String filename, ConfigValue value, Object o) throws ValueException {
        if (o instanceof Map m) {
            o = m.values();
        }
        if (o instanceof Collection c) {
            if (c.isEmpty()) {
                final String msg = "Cannot resolve generic parameters from empty collection";
                throw new ValueException(msg, filename, value);
            }
            final Object e = c.iterator().next();
            list.add(ConfigUtil.widen(e.getClass()));
            addGenericsFromValue(list, filename, value, e);
        } else if (o == null) {
            final String msg = "Cannot resolve generic parameters from null value";
            throw new ValueException(msg, filename, value);
        }
    }
}
