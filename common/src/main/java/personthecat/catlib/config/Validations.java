package personthecat.catlib.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import personthecat.catlib.config.Validation.GenericTyped;
import personthecat.catlib.config.Validation.NotNull;
import personthecat.catlib.config.Validation.Typed;

public record Validations(Map<Class<?>, Validation<?>> map, Class<?> type, Class<?>[] generics) {

    public static Validations fromValue(String filename, ConfigValue value) throws ValueException {
        final Map<Class<?>, Validation<?>> map = mapValidations(filename, value);
        final Class<?>[] generics = resolveGenerics(filename, map, value);
        confirmGenerics(filename, value, generics);
        generateValidations(map, value);
        return new Validations(map, value.type(), generics);
    }

    private static Map<Class<?>, Validation<?>> mapValidations(String filename, ConfigValue value) throws ValueException {
        final Map<Class<?>, Validation<?>> map = new HashMap<>();
        for (final Validation<?> v : value.validations()) {
            if (map.put(v.getClass(), v) != null) {
                final String msg = "Multiple validations of type " + v.getClass().getSimpleName() + " declared on field";
                throw new ValueException(msg, filename, value);
            }
        }
        return map;
    }

    private static Class<?>[] resolveGenerics(
            String filename, Map<Class<?>, Validation<?>> map, ConfigValue value) throws ValueException {
        if (!ConfigUtil.isSupportedGenericType(value.type())) return new Class<?>[0];
        if (!map.containsKey(GenericTyped.class)) {
            final Class<?>[] types = ConfigUtil.getGenericTypes(filename, value);
            if (types.length > 0) {
                map.put(GenericTyped.class, new GenericTyped(types));
            }
            return types;
        }
        return ((GenericTyped) map.get(GenericTyped.class)).types();
    }

    private static void confirmGenerics(
            String filename, ConfigValue value, Class<?>[] generics) throws ValueException {
        if (ConfigUtil.isSupportedGenericType(value.type())) {
            if (generics.length == 0) {
                final String msg = "Validations did not generate correctly. Expected at least 1 generic type";
                throw new ValueException(msg, filename, value);
            }
        }
    }

    private static void generateValidations(Map<Class<?>, Validation<?>> map, ConfigValue value) {
        if (!value.canBeNull()) {
            map.computeIfAbsent(Validation.NotNull.class, c -> new Validation.NotNull());
        }
        map.computeIfAbsent(Typed.class, c -> new Typed<>(ConfigUtil.widen(value.type())));
    }

    public Class<?> genericType() {
        return this.generics.length == 0 ? Void.class : this.generics[0];
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
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

    public void clear() {
        this.map.clear();
    }

    public Validations cloneValidations() {
        return new Validations(new HashMap<>(this.map), this.type, this.generics);
    }

    public Collection<Validation<?>> values() {
        return this.map.values();
    }

    public Predicate<Object> typeValidator() {
        return this.createValidator(this.getTypeValidations(this.map));
    }

    public Predicate<Object> entryTypeValidator() {
        return this.createValidator(this.getTypeValidations(this.entryValidations()));
    }

    private List<Validation<?>> getTypeValidations(Map<Class<?>, Validation<?>> map) {
        return map.values().stream()
            .filter(v -> v instanceof Typed || v instanceof GenericTyped || v instanceof NotNull)
            .toList();
    }

    public Map<Class<?>, Validation<?>> entryValidations() {
        final Class<?>[] generics = this.generics;
        if (generics.length == 0) {
            return this.map;
        }
        final Map<Class<?>, Validation<?>> map = new HashMap<>(this.map);
        map.put(Typed.class, new Typed<>(ConfigUtil.widen(generics[0])));
        if (generics.length > 1) {
            final Class<?>[] remainingGenerics = ConfigUtil.shiftGenerics(generics);
            map.put(GenericTyped.class, new GenericTyped(remainingGenerics));
        }
        return map;
    }

    private Predicate<Object> createValidator(Collection<Validation<?>> list) {
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
