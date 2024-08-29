package personthecat.catlib.data.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A map storing multiple keys per value in the form of a {@link List}.
 *
 * @param <K> The type of key.
 * @param <V> The type of value.
 */
@SuppressWarnings("unused")
public interface MultiValueMap<K, V> extends Map<K, List<V>> {

    /**
     * Adds a value to the underlying collection for this key. Note that the collection
     * will be created, if absent.
     *
     * @param k The key which the given value will be mapped to.
     * @param v The value to be inserted for this key.
     */
    default void add(final K k, final V v) {
        if (!this.containsKey(k)) {
            this.put(k, this.createList());
        }
        this.get(k).add(v);
    }

    /**
     * Adds multiple values to the underlying collection for this key. Note that the
     * collection will be created, if absent.
     *
     * @param k The key which the given value will be mapped to.
     * @param vs The values to be inserted for this key.
     */
    default void addAll(final K k, final Collection<V> vs) {
        if (!this.containsKey(k)) {
            this.put(k, this.createList());
        }
        this.get(k).addAll(vs);
    }

    /**
     * Creates a new list when adding to an unmapped key.
     *
     * @return A new List of type <code>V</code>.
     */
    default List<V> createList() {
        return new ArrayList<>();
    }

    /**
     * Removes a value from the underlying collection for this key. This implementation
     * is optimized to remove the entire key from the map if the resulting list is empty.
     *
     * @param k The key which the given value is mapped to.
     * @param v The value to be removed for this key.
     */
    default void removeNeatly(final K k, final V v) {
        final var list = this.get(k);
        if (list != null) {
            list.remove(v);
            if (list.isEmpty()) {
                this.remove(k);
            }
        }
    }

    /**
     * Removes a value from the underlying collection for this key. This implementation
     * is optimized to remove the entire key from the map if the resulting list is empty.
     *
     * @param k The key which the given value is mapped to.
     * @param vs The values to be removed for this key.
     */
    default void removeAllNeatly(final K k, final Collection<V> vs) {
        final var list = this.get(k);
        if (list != null) {
            list.removeAll(vs);
            if (list.isEmpty()) {
                this.remove(k);
            }
        }
    }

    /**
     * Runs a function for each value in every container.
     *
     * @param f A function consuming all keys and each value mapped to them.
     */
    default void forEachInner(final BiConsumer<K, V> f) {
        for (final Map.Entry<K, List<V>> entry : this.entrySet()) {
            for (final V v : entry.getValue()) {
                f.accept(entry.getKey(), v);
            }
        }
    }
}
