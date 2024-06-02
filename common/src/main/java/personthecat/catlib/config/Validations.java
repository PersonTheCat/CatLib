package personthecat.catlib.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import personthecat.catlib.config.Validation.GenericTyped;
import personthecat.catlib.config.Validation.Typed;

public record Validations(Map<Class<?>, Validation<?>> map, Class<?> type, Class<?>[] generics) {

    public static Validations fromValue(String filename, ConfigValue value) throws ValueException {
        final Map<Class<?>, Validation<?>> map = mapValidations(filename, value);
        if (!value.canBeNull()) {
            map.computeIfAbsent(Validation.NotNull.class, c -> new Validation.NotNull());
        }
        final Class<?>[] generics = resolveGenerics(filename, map, value);
        if (Collection.class.isAssignableFrom(value.type())) {
            map.put(Typed.class, new Typed<>(ConfigUtil.widen(generics[0])));
            if (generics.length > 1) {
                final Class<?>[] remainingGenerics = ConfigUtil.shiftGenerics(generics);
                map.put(GenericTyped.class, new GenericTyped(remainingGenerics));
            }
        } else {
            map.computeIfAbsent(Typed.class, c -> new Typed<>(ConfigUtil.widen(value.type())));
        }
        return new Validations(map, value.type(), generics);
    }

    static Map<Class<?>, Validation<?>> mapValidations(String filename, ConfigValue value) throws ValueException {
        final Map<Class<?>, Validation<?>> map = new HashMap<>();
        for (final Validation<?> v : value.validations()) {
            if (map.put(v.getClass(), v) != null) {
                final String msg = "Multiple validations of type " + v.getClass().getSimpleName() + " declared on field";
                throw new ValueException(msg, filename, value);
            }
        }
        return map;
    }

    static Class<?>[] resolveGenerics(
            String filename, Map<Class<?>, Validation<?>> map, ConfigValue value) throws ValueException {
        if (value.type().getTypeParameters().length == 0) return new Class<?>[0];
        if (!map.containsKey(GenericTyped.class)) {
            final Class<?>[] types = ConfigUtil.getGenericTypes(filename, value);
            if (types.length > 0) {
                map.put(GenericTyped.class, new GenericTyped(types));
            }
            return types;
        }
        return ((GenericTyped) map.get(GenericTyped.class)).types();
    }

    @SuppressWarnings("unchecked")
    public <T extends Validation<?>> T get(Class<T> type) {
        return (T) this.map.get(type);
    }

    public <T extends Validation<?>> T get(Class<T> type, Supplier<T> def) {
        final T t = this.get(type);
        return t != null ? t : def.get();
    }

    @SuppressWarnings("unchecked")
    public <T extends Validation<?>> T take(Class<T> type) {
        return (T) this.map.remove(type);
    }

    public <T extends Validation<?>> T take(Class<T> type, Supplier<T> def) {
        final T t = this.take(type);
        return t != null ? t : def.get();
    }

    public Validation<?> set(Class<?> type, Validation<?> v) {
        return this.map.put(type, v);
    }

    public Predicate<Object> validator() {
        final List<Validation<?>> list = new ArrayList<>(this.map.values());
        if (this.requiresExactType()) {
            return o -> Validation.isValid(list, ConfigUtil.remap(this.type, this.generics, o));
        }
        return o -> Validation.isValid(list, o);
    }

    public boolean requiresExactType() {
        for (final Validation<?> v : this.map.values()) {
            if (v.requiresExactType()) {
                return true;
            }
        }
        return false;
    }
}
