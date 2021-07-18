package personthecat.catlib.data;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.function.Consumer;

@Immutable
@SuppressWarnings("unused")
public class SequentialDualHashMap<K1, K2, V> implements DualMap<K1, K2, V> {

    protected final Map<K1, Map<K2, V>> map;
    protected final List<V> values;

    private SequentialDualHashMap(final Map<K1, Map<K2, V>> map, final List<V> values) {
        this.map = map;
        this.values = values;
    }

    public Builder<K1, K2, V> builder() {
        return new Builder<>();
    }

    @Override
    public DualMap<K1, K2, V> forEach(final TriConsumer<K1, K2, V> f) {
        for (final Map.Entry<K1, Map<K2, V>> e1 : this.map.entrySet()) {
            for (final Map.Entry<K2, V> e2 : e1.getValue().entrySet()) {
                f.accept(e1.getKey(), e2.getKey(), e2.getValue());
            }
        }
        return this;
    }

    @Override
    public DualMap<K1, K2, V> forEach(final Consumer<V> f) {
        this.values.forEach(f);
        return this;
    }

    @Nullable
    @Override
    public V get(final K1 k1, final K2 k2) {
        final Map<K2, V> k2Get = this.map.get(k1);
        if (k2Get != null) {
            return k2Get.get(k2);
        }
        return null;
    }

    @Override
    public boolean containsKeys(final K1 k1, final K2 k2) {
        final Map<K2, V> k2Get = this.map.get(k1);
        return k2Get != null && k2Get.containsKey(k2);
    }

    @Override
    public boolean containsValue(final V v) {
        return this.values.contains(v);
    }

    @Override
    public int size() {
        return this.values.size();
    }

    @Override
    @Deprecated
    public DualMap<K1, K2, V> put(final K1 k1, final K2 k2, final V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public DualMap<K1, K2, V> putAll(final DualMap<K1, K2, V> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean remove(final K1 k1, final K2 k2) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean remove(final V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @NotThreadSafe
    public static class Builder<K1, K2, V> extends SequentialDualHashMap<K1, K2, V> {

        private final Map<V, Pair<K1, K2>> reverseLookup = new HashMap<>();

        Builder() {
            super(new HashMap<>(), new ArrayList<>());
        }

        @Override
        @SuppressWarnings("deprecation")
        public DualMap<K1, K2, V> put(final K1 k1, final K2 k2, final V v) {
            this.map.putIfAbsent(k1, new HashMap<>());
            this.values.add(v);
            this.reverseLookup.put(v, Pair.of(k1, k2));
            return this;
        }

        @Override
        @SuppressWarnings("deprecation")
        public DualMap<K1, K2, V> putAll(final DualMap<K1, K2, V> map) {
            map.forEach(this::put);
            return this;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean remove(final K1 k1, final K2 k2) {
            final Map<K2, V> k2Get = this.map.get(k1);
            if (k2Get == null) {
                return false;
            }
            final V v = k2Get.remove(k2);
            return v != null && this.values.remove(v);
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean remove(final V v) {
            final Pair<K1, K2> keys = this.reverseLookup.get(v);
            final Map<K2, V> lookup = this.map.get(keys.getLeft());
            return this.values.remove(v) && lookup.remove(keys.getRight()) != null;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void clear() {
            this.map.clear();
            this.values.clear();
            this.reverseLookup.clear();
        }

        public SequentialDualHashMap<K1, K2, V> build() {
            return new SequentialDualHashMap<>(immutableCopyOf(this.map), Collections.unmodifiableList(this.values));
        }

        private static <K1, K2, V> Map<K1, Map<K2, V>> immutableCopyOf(final Map<K1, Map<K2, V>> map) {
            final ImmutableMap.Builder<K1, Map<K2, V>> immutable = ImmutableMap.builder();
            map.forEach((k1, k2vMap) -> immutable.put(k1, Collections.unmodifiableMap(k2vMap)));
            return immutable.build();
        }
    }
}
