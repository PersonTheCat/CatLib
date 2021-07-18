package personthecat.catlib.data;

import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.util.function.Consumer;

/**
 * Represents a kind of map which accepts two keys.
 *
 * @param <K1> The first key type.
 * @param <K2> The second key type.
 * @param <V> The mapped value type.
 */
public interface DualMap<K1, K2, V> {

    DualMap<K1, K2, V> put(final K1 k1, final K2 k2, final V v);

    DualMap<K1, K2, V> putAll(final DualMap<K1, K2, V> map);

    boolean remove(final K1 k1, final K2 k2);

    boolean remove(final V v);

    DualMap<K1, K2, V> forEach(final TriConsumer<K1, K2, V> f);

    DualMap<K1, K2, V> forEach(final Consumer<V> f);

    @Nullable
    V get(final K1 k1, final K2 k2);

    @CheckReturnValue
    boolean containsKeys(final K1 k1, final K2 k2);

    @CheckReturnValue
    boolean containsValue(final V v);

    @CheckReturnValue
    int size();

    @CheckReturnValue
    void clear();

    @CheckReturnValue
    default boolean isEmpty() {
        return this.size() == 0;
    }
}
