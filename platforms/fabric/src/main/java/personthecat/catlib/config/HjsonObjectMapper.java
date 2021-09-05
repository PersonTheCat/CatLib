package personthecat.catlib.config;

import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.exception.NonSerializableObjectException;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.Shorthand;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

class HjsonObjectMapper {

    static void serializeObject(final Path p, final Object o) throws IOException, NonSerializableObjectException {
        HjsonUtils.writeJson(toJsonObject(o), p.toFile()).throwIfErr();
    }

    static <T> T deserializeObject(final Path p, final Class<T> clazz) throws NonSerializableObjectException {
        final T t = Utils.constructUnsafely(clazz);

        final Optional<JsonObject> read = HjsonUtils.readJson(p.toFile());
        if (!read.isPresent()) return t;
        final JsonObject json = read.get();
        if (json.isEmpty()) return t;

        writeObjectInto(t, json);

        return t;
    }

    static JsonValue toJsonValue(final Object o) throws NonSerializableObjectException {
        if (o.getClass().isArray()) {
            return toJsonArray((Object[]) o);
        } else if (o.getClass().isEnum()) {
            return JsonValue.valueOf(((Enum<?>) o).name());
        } else if (o instanceof String) {
            return JsonValue.valueOf((String) o);
        } else if (o instanceof Integer) {
            return JsonValue.valueOf(((Integer) o).intValue());
        } else if (o instanceof Long) {
          return JsonValue.valueOf(((Long) o).longValue());
        } else if (o instanceof Float || o instanceof Double) {
            return JsonValue.valueOf(((Number) o).doubleValue());
        } else if (o instanceof Boolean) {
            return JsonValue.valueOf(((Boolean) o).booleanValue());
        } else if (o instanceof Collection) {
            return toJsonArray((Collection<?>) o);
        } else if (o instanceof Map) {
            return toJsonObject((Map<?, ?>) o);
        }
        return toJsonObject(o);
    }

    static JsonObject toJsonObject(final Object o) throws NonSerializableObjectException {
        final JsonObject json = new JsonObject();

        final Class<?> c = o.getClass();
        for (final Field f : c.getDeclaredFields()) {
            if (!Modifier.isFinal(f.getModifiers())) {
                final JsonValue value = toJsonValue(Utils.getUnsafely(f, o));

                final String comment = getComment(f);
                if (comment != null) value.setComment(comment);

                json.add(f.getName(), value);
            }
        }
        return json;
    }

    @Nullable
    static String getComment(final Field f) {
        final Comment[] comments = f.getAnnotationsByType(Comment.class);
        if (comments.length == 0) return null;
        return comments[0].value();
    }

    static JsonObject toJsonObject(final Map<?, ?> map) throws NonSerializableObjectException {
        final JsonObject json = new JsonObject();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw NonSerializableObjectException.unsupportedKey(entry.getKey());
            }
            json.add((String) entry.getKey(), toJsonValue(entry.getValue()));
        }
        return json;
    }

    static JsonArray toJsonArray(final Object[] a) throws NonSerializableObjectException {
        final JsonArray json = new JsonArray();
        for (final Object o : a) {
            json.add(toJsonValue(o));
        }
        return json;
    }

    static JsonArray toJsonArray(final Collection<?> a) throws NonSerializableObjectException {
        final JsonArray json = new JsonArray();
        if (a.isEmpty()) return json;
        for (final Object o : a) {
            json.add(toJsonValue(o));
        }
        return json;
    }

    private static void writeObjectInto(final Object o, final JsonObject json) throws NonSerializableObjectException {
        final Class<?> clazz = o.getClass();
        for (final JsonObject.Member member : json) {
            final Field f = getField(clazz, member.getName());
            if (f == null) continue;

            final Object def = Utils.getUnsafely(f, o);
            Utils.setUnsafely(f, o, getValueByType(f.getType(), def, member.getValue()));
        }
    }

    private static Field getField(final Class<?> clazz, final String name) {
        for (final Field f : clazz.getDeclaredFields()) {
            if (name.equals(f.getName())) {
                return f;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object getValueByType(final Class<?> type, final Object def, final JsonValue value) throws NonSerializableObjectException {
        if (type.isArray()) {
            return toArray(type, def, value);
        } else if (type.isEnum()) {
            return Shorthand.assertEnumConstant(value.asString(), (Class) type);
        } else if (type.isAssignableFrom(String.class)) {
            return value.asString();
        } else if (type.isAssignableFrom(Integer.class) || type.isAssignableFrom(int.class)) {
            return value.asInt();
        } else if (type.isAssignableFrom(Long.class) || type.isAssignableFrom(long.class)) {
            return value.asLong();
        } else if (type.isAssignableFrom(Float.class) || type.isAssignableFrom(float.class)) {
            return value.asFloat();
        } else if (type.isAssignableFrom(Double.class) || type.isAssignableFrom(double.class)) {
            return value.asDouble();
        } else if (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(boolean.class)) {
            return value.asBoolean();
        } else if (type.isAssignableFrom(List.class)) {
            return toList(value, def);
        } else if (type.isAssignableFrom(Set.class)) {
            return new HashSet<>(toList(value, def));
        } else if (type.equals(Collection.class)) {
            return toList(value, def);
        } else if (type.isAssignableFrom(Map.class)) {
            return toMap(value, def);
        }
        final Object o = Utils.constructUnsafely(type);
        writeObjectInto(o, value.asObject());
        return o;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object toArray(final Class<?> type, Object def, final JsonValue value) throws NonSerializableObjectException {
        final JsonArray json = value.asArray();
        final Object[] array = new Object[json.size()];

        final Object[] defaults = (Object[]) def;
        def = defaults != null && defaults.length > 0 ? defaults[0] : null;

        for (int i = 0; i < json.size(); i++) {
            array[i] = getValueByType(type.getComponentType(), def, json.get(i));
        }
        return Arrays.copyOf(array, array.length, (Class) type);
    }

    private static List<Object> toList(final JsonValue value, Object def) throws NonSerializableObjectException {
        final Collection<?> defaults = (Collection<?>) def;
        def = defaults != null && !defaults.isEmpty() ? defaults.iterator().next() : null;

        if (def == null) throw NonSerializableObjectException.defaultRequired();

        final List<Object> list = new ArrayList<>();
        for (final JsonValue v : value.asArray()) {
            list.add(getValueByType(def.getClass(), def, v));
        }
        return list;
    }

    private static Map<String, Object> toMap(final JsonValue value, Object def) throws NonSerializableObjectException {
        final Map<String, Object> map = new HashMap<>();

        final Map<?, ?> defaults = (Map<?, ?>) def;
        def = defaults != null && !defaults.isEmpty() ? defaults.entrySet().iterator().next().getValue() : null;

        if (def == null) throw NonSerializableObjectException.defaultRequired();

        for (final JsonObject.Member member : value.asObject()) {
            map.put(member.getName(), getValueByType(def.getClass(), def, member.getValue()));
        }
        return map;
    }
}
