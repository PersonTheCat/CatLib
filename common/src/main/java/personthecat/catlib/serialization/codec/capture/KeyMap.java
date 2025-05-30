package personthecat.catlib.serialization.codec.capture;

import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.collections.MultiValueHashMap;
import personthecat.catlib.data.collections.MultiValueMap;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

class KeyMap<T> extends HashMap<Key<?>, T> {
    private final MultiValueMap<String, Entry<Key<?>, T>> byName = new MultiValueHashMap<>();

    @Override
    public T put(Key<?> key, T value) {
        final var original = super.put(key, value);
        this.byName.add(key.name(), new SimpleEntry<>(key, value));
        return original;
    }

    @Override
    public T putIfAbsent(Key<?> key, T value) {
        final var e = this.getMatching(key);
        return e != null ? e.getValue() : this.put(key, value);
    }

    @Override
    public T get(Object key) {
        var v = super.get(key);
        if (v == null && key instanceof Key<?> k) {
            final var e = this.getMatching(k);
            if (e != null) {
                return e.getValue();
            }
        }
        return v;
    }

    // Unlike HashMap, cannot support null values
    @Override
    public T getOrDefault(Object key, T def) {
        final T t = this.get(key);
        return t != null ? t : def;
    }

    @Override
    public boolean containsKey(Object key) {
        if (super.containsKey(key)) {
            return true;
        } else if (key instanceof Key<?> k) {
            return this.getMatching(k) != null;
        }
        return false;
    }

    @Override
    public T remove(Object key) {
        final var removed = super.remove(key);
        if (removed != null) {
            this.byName.removeNeatly(((Key<?>) key).name(), new SimpleEntry<>((Key<?>) key, removed));
            return removed;
        } else if (key instanceof Key<?> k) {
            final var e = this.getMatching(k);
            if (e != null && super.remove(e.getKey()) != null) {
                this.byName.removeNeatly(e.getKey().name(), e);
                return e.getValue();
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object key, Object value) {
        final boolean removed = super.remove(key, value);
        if (removed) {
            this.byName.removeNeatly(((Key<?>) key).name(), new SimpleEntry<>((Key<?>) key, (T) value));
            return true;
        } else if (key instanceof Key<?> k) {
            final var e = this.getMatching(k);
            if (e != null && super.remove(e.getKey(), value)) {
                this.byName.removeNeatly(e.getKey().name(), new SimpleEntry<>(e.getKey(), (T) value));
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        this.byName.clear();
    }

    private Entry<Key<?>, T> getMatching(Key<?> lookup) {
        for (final var entry : this.byName.getOrEmpty(lookup.name())) {
            if (keyMatches(entry.getKey(), lookup)) {
                return entry;
            }
        }
        final var nameless = lookup.asAny();
        final var t = super.get(nameless);
        if (t != null) {
            return new SimpleEntry<>(nameless, t);
        }
        return null;
    }

    // support unqualified and nameless values by qualified key (not vice versa)
    // e.g. if bound to Key.of('s', String), can find by Key.of(qfr, 's', String)
    // e.g. if bound to key.of(ANY, String), can find by Key.of('s', String)
    private static boolean keyMatches(Key<?> bound, Key<?> lookup) {
        return lookup.type().isAssignableFrom(bound.type())
            && (bound.name().equals(Key.ANY) || bound.name().equals(lookup.name()))
            && (!bound.isQualified() || Objects.equals(bound.qualifier(), lookup.qualifier()));
    }

    // unsupported operations (until needed or API exposed)

    @Override
    public T compute(Key<?> key, @NotNull BiFunction<? super Key<?>, ? super T, ? extends T> remappingFunction) {
        throw new UnsupportedOperationException("KeyMap#compute");
    }

    @Override
    public T computeIfAbsent(Key<?> key, @NotNull Function<? super Key<?>, ? extends T> mappingFunction) {
        throw new UnsupportedOperationException("KeyMap#computeIfAbsent");
    }

    @Override
    public T computeIfPresent(Key<?> key, @NotNull BiFunction<? super Key<?>, ? super T, ? extends T> remappingFunction) {
        throw new UnsupportedOperationException("KeyMap#computeIfPresent");
    }

    @Override
    public boolean replace(Key<?> key, T oldValue, T newValue) {
        throw new UnsupportedOperationException("KeyMap#replace");
    }

    @Override
    public T replace(Key<?> key, T value) {
        throw new UnsupportedOperationException("KeyMap#replace");
    }

    @Override
    public T merge(Key<?> key, @NotNull T value, @NotNull BiFunction<? super T, ? super T, ? extends T> remappingFunction) {
        throw new UnsupportedOperationException("KeyMap#merge");
    }
}
