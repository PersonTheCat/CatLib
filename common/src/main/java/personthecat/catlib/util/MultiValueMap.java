package personthecat.catlib.util;

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
    void add(final K k, final V v);

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
