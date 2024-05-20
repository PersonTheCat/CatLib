package personthecat.catlib.data.collections;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.annotation.concurrent.ThreadSafe;
import personthecat.catlib.data.Lazy;
import personthecat.catlib.data.ResettableLazy;
import personthecat.catlib.exception.MissingElementException;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static personthecat.catlib.exception.Exceptions.missingElement;

/**
 * A non-redundant set of objects which can only be written to the first time it is referenced.
 * In OSV 6.2, this type was modified to support registry keys and be resettable. Resetting this
 * registry is expected to be thread-safe, but may need further testing.
 *
 * <p>This is the primary type of registry used in this mod. This registry is lazily initialized
 * and cannot be modified. It is constructed with a data supplier which is then treated as an
 * event. This event will fire at the first time any of this class' methods are called. It is
 * equipped with each of the necessary overrides to be treated as a regular {@link Set}.</p>
 *
 * <p>Many of these methods are simply stubs which will throw an {@link UnsupportedOperationException}
 * when called. They are not intended for use by external implementors.</p>
 *
 * @param <K> The type of key in the registry.
 * @param <V> The type of value in the registry.
 */
@ThreadSafe
@SuppressWarnings("unused")
public class LazyRegistry<K, V> implements Map<K, V>, Iterable<V> {

    /**
     * The default error to throw on {@link LazyRegistry#getAsserted}
     */
    private static final ErrorFunction<Object> DEFAULT_ERROR = k ->
        missingElement("No value for key: {}", k);

    /**
     * An error to throw on {@link LazyRegistry#getAsserted}
     */
    private final ErrorFunction<K> err;

    /**
     * The underlying map whose contents will be filled on first use.
     */
    private final Lazy<Map<K, V>> map;

    /**
     * Constructs Lazy ImmutableSet of objects which will be filled upon first use.
     *
     * @param map The underlying, lazily-initialized contents of this registry.
     * @param err A function for generating missing element exceptions.
     */
    private LazyRegistry(final Lazy<Map<K, V>> map, final ErrorFunction<K> err) {
        this.map = map;
        this.err = err;
    }

    /**
     * Generates a new, immutable registry when given an event which supplies the
     * contents of this registry on first use.
     *
     * @param event Supplies the contents of this registry when it is first used.
     * @param <K> The type of key being stored in the registry.
     * @param <V> The type of value being stored in the registry.
     * @return An immutable registry which cannot be refreshed.
     */
    public static <K, V> LazyRegistry<K, V> of(final Supplier<Map<K, V>> event) {
        return of(event, DEFAULT_ERROR::apply);
    }

    /**
     * Variant of {@link #of(Supplier)} in which the data have already been supplied.
     *
     * @param map The contents of this registry, already available.
     * @param <K> The type of key being stored in the registry.
     * @param <V> The type of value being stored in the registry.
     * @return An immutable registry which cannot be refreshed.
     */
    public static <K, V> LazyRegistry<K, V> of(final Map<K, V> map) {
        return new LazyRegistry<>(new Lazy<>(Collections.unmodifiableMap(map)), DEFAULT_ERROR::apply);
    }

    /**
     * Variant of {@link #of(Supplier)} which accepts a function for generating
     * missing element exceptions.
     *
     * @param event Supplies the contents of this registry when it is first used.
     * @param err A function for generating missing element exceptions.
     * @param <K> The type of key being stored in the registry.
     * @param <V> The type of value being stored in the registry.
     * @return An immutable registry which cannot be refreshed.
     */
    public static <K, V> LazyRegistry<K, V> of(final Supplier<Map<K, V>> event, final ErrorFunction<K> err) {
        return new LazyRegistry<>(new Lazy<>(() -> Collections.unmodifiableMap(event.get())), err);
    }

    /**
     * Generates a new registry where keys are insignificant. Instead, the values
     * will be enumerated.
     *
     * @param event Supplies the contents of this registry when it is first used.
     * @param <V> The type of value being stored in the registry.
     * @return An immutable registry which cannot be refreshed.
     */
    public static <V> LazyRegistry<Integer, V> enumerated(final Supplier<Collection<V>> event) {
        return enumerated(event, DEFAULT_ERROR::apply);
    }

    /**
     * Variant of {@link #enumerated(Supplier)} in which the data have already been supplied.
     *
     * @param data The contents of this registry, already available.
     * @param <V> The type of value being stored in the registry.
     * @return An immutable registry which cannot be refreshed.
     */
    public static <V> LazyRegistry<Integer, V> enumerated(final Collection<V> data) {
        return new LazyRegistry<>(new Lazy<>(enumerate(data)), DEFAULT_ERROR::apply);
    }

    /**
     * Variant of {@link #enumerated(Supplier)} which accepts a function for generating
     * missing element exceptions.
     *
     * @param event Supplies the contents of this registry when it is first used.
     * @param err A function for generating missing element exceptions.
     * @param <V> The type of value being stored in the registry.
     * @return An immutable registry which cannot be refreshed.
     */
    public static <V> LazyRegistry<Integer, V> enumerated(final Supplier<Collection<V>> event, final ErrorFunction<Integer> err) {
        return of(() -> enumerate(event.get()), err);
    }

    /**
     * Enumerates every value in a collection, mapping them to their index.
     *
     * @param values A collection of any type.
     * @param <V> The type of value being stored in the registry.
     * @return An {@link ImmutableMap} containing the original values.
     */
    private static <V> ImmutableMap<Integer, V> enumerate(final Collection<V> values) {
        final ImmutableMap.Builder<Integer, V> map = ImmutableMap.builder();
        int i = 0;
        for (V value : values) {
            map.put(i++, value);
        }
        return map.build();
    }

    /**
     * A static utility for loading a series of multiple {@link LazyRegistry registries}.
     *
     * @param registries A series of registries being initialized at this time.
     */
    public static void loadAll(final LazyRegistry<?, ?>... registries) {
        for (final LazyRegistry<?, ?> registry : registries) {
            registry.load();
        }
    }

    /**
     * A static utility for unloading a series of multiple {@link LazyRegistry registries}.
     *
     * <p>Note that non-resettable registries will neither unload nor throw exceptions.</p>
     *
     * @param registries A series of registries being unloaded at this time.
     */
    public static void resetAll(final LazyRegistry<?, ?>... registries) {
        for (final LazyRegistry<?, ?> registry : registries) {
            registry.tryReset();
        }
    }

    /**
     * A static utility for reloading a series of multiple {@link LazyRegistry registries}.
     *
     * <p>Note that non-resettable registries will neither reload nor throw exceptions.</p>
     *
     * @param registries A series of registries being reloaded at this time.
     */
    public static void reloadAll(final LazyRegistry<?, ?>... registries) {
        for (final LazyRegistry<?, ?> registry : registries) {
            registry.tryReload();
        }
    }

    /**
     * Generates a new registry containing the same data. The new registry will
     * respond with this message when calling {@link #getAsserted(Object)}.
     *
     * @param err A function for generating missing element exceptions.
     * @return A new registry with the updated error info.
     */
    public LazyRegistry<K, V> respondsWithError(final ErrorFunction<K> err) {
        return new LazyRegistry<>(this.map, err);
    }

    /**
     * Variant of {@link #respondsWithError(ErrorFunction)} in which an error message
     * is wrapped in a standard {@link MissingElementException}.
     *
     * @param msg A function for generating error messages.
     * @return A new registry with the updated error info.
     */
    public LazyRegistry<K, V> respondsWith(final Function<K, String> msg) {
        return new LazyRegistry<>(this.map, k -> missingElement(msg.apply(k)));
    }

    /**
     * Converts this registry into a resettable registry or explicitly marks it
     * as non-resettable.
     *
     * @param resettable Whether this registry can now be reloaded.
     * @return A new registry which can reloaded.
     */
    public LazyRegistry<K, V> canBeReset(final boolean resettable) {
        if (resettable == this.map.isResettable()) {
            return this;
        }
        return new LazyRegistry<>(this.map.asResettable(resettable), this.err);
    }

    /**
     * Returns the value mapped to the given key, wrapped in {@link Optional}.
     *
     * @param k The key which an expected value is mapped to.
     * @return The researched value, or else {@link Optional#empty}.
     */
    public Optional<V> getOptional(final K k) {
        return Optional.ofNullable(this.get(k));
    }

    /**
     * Variant of {@link #getOptional(Object)} which instead throws an exception if
     * no value is mapped to the given key.
     *
     * @throws RuntimeException If no value is present.
     * @param k The key which an expected value is mapped to.
     * @return The researched value.
     */
    public V getAsserted(final K k) {
        final V v = this.get(k);
        if (v == null) {
            throw this.err.apply(k);
        }
        return v;
    }

    /**
     * Returns a set of matching values in this registry for the given keys.
     *
     * @param ks The keys for each element being retrieved.
     * @return A set containing the matching values in the registry.
     */
    public Set<V> tryGetAll(final Collection<K> ks) {
        final Set<V> set = new HashSet<>();
        ks.forEach(k -> this.getOptional(k).ifPresent(set::add));
        return set;
    }

    /**
     * Variant of {@link #tryGetAll(Collection)} which asserts that all keys are
     * mapped in this registry.
     *
     * @param ks The keys for each element being retrieved.
     * @return A set containing the matching values in the registry.
     */
    public Set<V> getAllAsserted(final Collection<K> ks) {
        return ks.stream().map(this::getAsserted).collect(Collectors.toSet());
    }

    /**
     * Returns the value corresponding to whichever key matches the given predicate.
     *
     * @param predicate The predicate used to determine which key to accept.
     * @return The expected value, or else {@link Optional#empty}.
     */
    public Optional<V> findByKey(final Predicate<K> predicate) {
        for (final Map.Entry<K, V> entry : this.entrySet()) {
            if (predicate.test(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the value corresponding to the given predicate.
     *
     * @param predicate The predicate used to determine which value to accept.
     * @return The expected value, or else {@link Optional#empty()}.
     */
    public Optional<V> findByValue(final Predicate<V> predicate) {
        for (final V v : this.values()) {
            if (predicate.test(v)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    /**
     * Loads the underlying hash table immediately. This can be called after
     * constructing the registry to have it load on construction instead of
     * lazily.
     *
     * <p>For example,</p>
     * <pre>{@code
     *   final SafeRegistry<Key, Item> ITEMS =
     *     SafeRegistry.of(ItemInit::loadItems)
     *       .canBeReset(true)
     *       .load();
     * }</pre>
     *
     * @return <code>this</code>, for method chaining.
     */
    public LazyRegistry<K, V> load() {
        this.map.get();
        return this;
    }

    /**
     * Attempts to reset the underlying map contained within the registry. If
     * the map cannot be reset, nothing will happen.
     *
     * @return <code>this</code>, for method chaining.
     */
    public LazyRegistry<K, V> tryReset() {
        if (this.map.isResettable()) {
            ((ResettableLazy<Map<K, V>>) this.map).reset();
        }
        return this;
    }

    /**
     * Variant of {@link #tryReset} which asserts that the registry can be reset.
     *
     * @throws UnsupportedOperationException If the registry is not resettable.
     * @return <code>this</code>, for method chaining.
     */
    public LazyRegistry<K, V> reset() {
        if (this.map.isResettable()) {
            ((ResettableLazy<Map<K, V>>) this.map).reset();
            return this;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to reload the data in this registry. If the underlying map cannot
     * be reset, nothing will happen.
     *
     * @return <code>this</code>, for method chaining.
     */
    public LazyRegistry<K, V> tryReload() {
        return this.tryReset().load();
    }

    /**
     * Variant of {@link #tryReload} which asserts that the registry can be reset.
     *
     * @throws UnsupportedOperationException If the registry is not resettable.
     * @return <code>this</code>, for method chaining.
     */
    public LazyRegistry<K, V> reload() {
        return this.reset().load();
    }

    /**
     * Recalculates and returns the up to date data contained within the registry.
     *
     * @return The recalculated data corresponding to this registry.
     */
    public Map<K, V> getUpdated() {
        return this.map.getUpdated();
    }

    /**
     * Determines whether this registry has been accessed or finished loading.
     *
     * @return <code>true</code>, if the registry has loaded.
     */
    public boolean hasLoaded() {
        return this.map.computed();
    }

    public Stream<V> stream() {
        return this.values().stream();
    }

    @Override
    public int size() {
        return this.map.get().size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.get().isEmpty();
    }

    @Override
    public boolean containsKey(final Object o) {
        return this.map.get().containsKey(o);
    }

    @Override
    public boolean containsValue(final Object o) {
        return this.map.get().containsValue(o);
    }

    @Nullable
    @Override
    public V get(final Object o) {
        return this.map.get().get(o);
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return this.map.get().keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return this.map.get().values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.map.get().entrySet();
    }

    @NotNull
    @Override
    public Iterator<V> iterator() {
        return this.map.get().values().iterator();
    }

    @Nullable
    @Override
    @Deprecated
    public V put(final K k, final V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public V remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void putAll(final @NotNull Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @FunctionalInterface
    public interface ErrorFunction<K> {
        RuntimeException apply(final K k);
    }

}