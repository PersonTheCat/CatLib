package personthecat.catlib.data;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/** @deprecated Experimental use only */
@Deprecated
public class ListeningMultiValueMap<K, V> implements MultiValueMap<K, V> {

    private final MultiValueMap<K, V> wrapped;
    private final BiConsumer<K, List<V>> onUpdate;

    public ListeningMultiValueMap(final MultiValueMap<K, V> wrapped, final BiConsumer<K, List<V>> onUpdate) {
        this.wrapped = wrapped;
        this.onUpdate = onUpdate;
    }

    @Override
    public void add(final K k, final V v) {
        this.wrapped.add(k, v);
        this.onUpdate.accept(k, this.get(k));
    }

    @Override
    public int size() {
        return this.wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return this.wrapped.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return this.wrapped.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return this.wrapped.containsValue(value);
    }

    @Override
    public List<V> get(final Object key) {
        return this.wrapped.get(key);
    }

    @Override
    public List<V> put(final K key, final List<V> value) {
        final List<V> ret = this.wrapped.put(key, value);
        this.onUpdate.accept(key, value);
        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<V> remove(final Object key) {
        final List<V> ret = this.wrapped.remove(key);
        if (ret != null) { // Type must be instanceof K
            this.onUpdate.accept((K) key, null);
        }
        return ret;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends List<V>> m) {
        for (final Entry<? extends K, ? extends List<V>> entry : m.entrySet()) {
            final List<V> original = this.put(entry.getKey(), entry.getValue());
            if (original != null) {
                this.onUpdate.accept(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void clear() {
        if (!this.wrapped.isEmpty()) {
            final Set<K> keys = this.keySet();
            this.wrapped.clear();
            for (final K key : keys) {
                this.onUpdate.accept(key, null);
            }
        }
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return this.wrapped.keySet();
    }

    @NotNull
    @Override
    public Collection<List<V>> values() {
        return this.wrapped.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, List<V>>> entrySet() {
        return this.wrapped.entrySet();
    }
}
