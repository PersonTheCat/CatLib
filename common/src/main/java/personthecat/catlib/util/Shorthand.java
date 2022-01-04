package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.exception.InvalidEnumConstantException;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;

import static java.util.Optional.empty;
import static personthecat.catlib.exception.Exceptions.invalidConstant;

/**
 * A collection of platform-agnostic <b>shorthand</b> utilities.
 */
@UtilityClass
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class Shorthand {

    /**
     * Shorthand for calling Optional#of, matching the existing syntax of
     * `empty`, while being more clear than `of` alone.
     *
     * @param val The value being wrapped.
     * @param <T> The type of value being wrapped.
     * @return <code>val</code>, wrapped in {@link Optional}.
     */
    @NotNull
    public static <T> Optional<T> full(final T val) {
        return Optional.of(val);
    }

    /**
     * Shorthand for calling Optional#ofNullable.
     *
     * @param val The value being wrapped.
     * @param <T> The type of value being wrapped.
     * @return <code>val</code>, wrapped in {@link Optional}.
     */
    @NotNull
    public static <T> Optional<T> nullable(final @Nullable T val) {
        return Optional.ofNullable(val);
    }

    /**
     * Returns a random number between the input bounds, inclusive.
     *
     * @param rand A random number generator of any kind.
     * @param min The minimum value, inclusive.
     * @param max The maximum value, inclusive.
     * @return A random number in this range.
     */
    public static int numBetween(final Random rand, final int min, final int max) {
        return min == max ? min : rand.nextInt(max - min + 1) + min;
    }

    /**
     * Returns a random number between the input bounds, inclusive.
     *
     * @param rand A random number generator of any kind.
     * @param min The minimum value, inclusive.
     * @param max The maximum value, inclusive.
     * @return A random number in this range.
     */
    public static float numBetween(final Random rand, final float min, final float max) {
        return min == max ? min : rand.nextFloat() * (max - min) + min;
    }

    /**
     * Returns 1 / x, or else {@link Integer#MAX_VALUE} if x == 0.
     * <p>
     *   This may be useful in cases where a random number is being generated
     *   in the following pattern, but a double is provided:
     * </p><pre>{@code
     *   // return true on 1 / x chance
     *   rand.nextInt(i) == 0
     * }</pre>
     *
     * @param value Any decimal value.
     * @return The inversion of the input, which never divides by 0.
     */
    public static int invert(final double value) {
        return value == 0 ? Integer.MAX_VALUE : (int) (1 / value);
    }

    /**
     * Returns 1 / x, or else 0 if x == 0;
     *
     * @param value Any integer value
     * @return The inversion of the input, which never divides by 0.
     */
    public static double invert(final int value) {
        return value == 0 ? 1.0 : 1.0 / (double) value;
    }

    /**
     * Variant of {@link #invert(double)} safely accepting null values.
     *
     * @param value Any double value, or <code>null</code>.
     * @return The inverse of the input, or else <code>null</code>.
     */
    @Contract("null -> null; !null -> !null")
    public static Integer invert(final @Nullable Double value) {
        return value != null ? invert(value.doubleValue()) : null;
    }

    /**
     * Variant of {@link #invert(int)} safely accepting null values.
     *
     * @param value Any integer value, or <code>null</code>.
     * @return The inverse of the input, or else <code>null</code>.
     */
    @Contract("null -> null; !null -> !null")
    public static Double invert(final @Nullable Integer value) {
        return value != null ? invert(value.intValue()) : null;
    }

    /**
     * Returns the first non-null argument, or else null.
     *
     * <p>One particular use case for this method which may come in handy is
     * when applying a large number of default values, as this will reduce
     * the number of extraneous tokens.
     *
     * <pre>{@code
     *   class Dog {
     *     final String name;
     *     final int age;
     *
     *     Dog(final @Nullable String name, final @Nullable Integer age) {
     *       this.name = coalesce(name, "Sparky");
     *       this.age = coalesce(age, 14);
     *     }
     *   }
     * }</pre>
     *
     * @param t1  The first argument being compared.
     * @param t2  The second argument being compared.
     * @param <T> The type of argument and return value.
     * @return The first non-null argument, or else null.
     */
    @Contract("null, null -> null; _, !null -> !null")
    public static <T> T coalesce(final @Nullable T t1, final @Nullable T t2) {
        return t1 != null ? t1 : t2;
    }

    /**
     * Variant of {@link #coalesce(Object, Object)} accepting 3 parameters.
     *
     * @param t1  The first argument being compared.
     * @param t2  The second argument being compared.
     * @param t3  The third argument being compared.
     * @param <T> The type of argument and return value.
     * @return The first non-null argument, or else null.
     */
    @Contract("null, null, null -> null; _, _, !null -> !null")
    public static <T> T coalesce(final @Nullable T t1, final @Nullable T t2, final @Nullable T t3) {
        if (t1 != null) return t1;
        if (t2 != null) return t2;
        return t3;
    }

    /**
     *  Variant of {@link #coalesce(Object, Object)} accepting 4 parameters.
     *
     * @param t1  The first argument being compared.
     * @param t2  The second argument being compared.
     * @param t3  The third argument being compared.
     * @param t4  The fourth argument being compared.
     * @param <T> The type of argument and return value.
     * @return The first non-null argument, or else null.
     */
    @Contract("null, null, null, null -> null; _, _, _, !null -> !null")
    public static <T> T coalesce(final @Nullable T t1, final @Nullable T t2, final @Nullable T t3, final @Nullable T t4) {
        if (t1 != null) return t1;
        if (t2 != null) return t2;
        if (t3 != null) return t3;
        return t4;
    }

    /**
     * Interpolates strings by replacing instances of <code>{}</code> in order.
     *
     * @param s The template string being formatted.
     * @param args A list of arguments to interpolate into this string.
     * @return A formatted, interpolated string.
     */
    @NotNull
    public static String f(final String s, final Object... args) {
        int begin = 0, si = 0, oi = 0;
        final StringBuilder sb = new StringBuilder();
        while (true) {
            si = s.indexOf("{}", si);
            if (si >= 0) {
                sb.append(s, begin, si);
                sb.append(args[oi++]);
                begin = si = si + 2;
            } else {
                break;
            }
        }
        sb.append(s.substring(begin));
        return sb.toString();
    }

    /**
     * Uses a linear search algorithm to locate a value in an array, matching
     * the predicate `by`. Shorthand for Stream#findFirst.
     *
     * <p>Example:</p>
     * <pre>{@code
     *    // Find x by x.name
     *    Object[] vars = getObjectsWithNames();
     *    Optional<Object> var = find(vars, (x) -> x.name.equals("Cat"));
     *    // You can then get the value -> NPE
     *    Object result = var.get()
     *    // Or use an alternative. Standard java.util.Optional. -> no NPE
     *    Object result = var.orElse(new Object("Cat"))
     * }</pre>
     *
     * @param <T> The type of array being passed in.
     * @param values The actual array containing the value.
     * @param by A predicate which determines which value to return.
     * @return The value, or else {@link Optional#empty}.
     */
    @NotNull
    public static <T> Optional<T> find(final T[] values, final Predicate<T> by) {
        for (final T val : values) {
            if (by.test(val)) {
                return full(val);
            }
        }
        return empty();
    }

    /**
     * Variant of {@link #find(Object[], Predicate)} which consumes any type of
     * {@link Iterable} instead.
     *
     * @param <T> The type of array being passed in.
     * @param values The actual array containing the value.
     * @param by A predicate which determines which value to return.
     * @return The value, or else {@link Optional#empty}.
     */
    @NotNull
    public static <T> Optional<T> find(final Iterable<T> values, final Predicate<T> by) {
        for (final T val : values) {
            if (by.test(val)) {
                return full(val);
            }
        }
        return empty();
    }

    /**
     * Variant of {@link #find(Object[], Predicate)} which consumes any type of
     * {@link Map} instead.
     *
     * @param <K> The unused key type in the given map.
     * @param <V> The type of value being retrieved.
     * @param map The map containing the value.
     * @param by A predicate which determines which value to return.
     * @return The value, or else {@link Optional#empty}.
     */
    @NotNull
    public static <K, V> Optional<V> find(final Map<K, V> map, final Predicate<V> by) {
        return find(map.values(), by);
    }

    /**
     * Variant of {@link #find(Object[], Predicate)} which returns all possible
     * matches when given an {@link Iterable}.
     *
     * @param <T> The type of array being passed in.
     * @param values The actual array containing the value.
     * @param by A predicate which determines which value to return.
     * @return The value, or else an empty list.
     */
    @NotNull
    public static <T> List<T> findAll(final Iterable<T> values, final Predicate<T> by) {
        final List<T> all = new ArrayList<>();
        for (T val : values) {
            if (by.test(val)) {
                all.add(val);
            }
        }
        return all;
    }

    /**
     * Variant of {@link #find(Map, Predicate)} which returns all possible matches.
     *
     * @param <K> The unused key type in the given map.
     * @param <V> The type of value being retrieved.
     * @param map The map containing the value.
     * @param by A predicate which determines which value to return.
     * @return The value, or else an empty list.
     */
    @NotNull
    public static <K, V> List<V> findAll(final Map<K, V> map, final Predicate<V> by) {
        return findAll(map.values(), by);
    }

    /**
     * Determines whether any value in the collection matches the predicate.
     *
     * @param <T> The type of array being passed in.
     * @param values The actual array containing the value.
     * @param by A predicate which determines which value to return.
     * @return Whether any matches are present in the object.
     */
    public static <T> boolean anyMatches(final T[] values, final Predicate<T> by) {
        for (T val : values) {
            if (by.test(val)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Variant of {@link #anyMatches(Object[], Predicate)} which consumes any
     * type of {@link Iterable}.
     *
     * @param <T> The type of array being passed in.
     * @param values The actual array containing the value.
     * @param by A predicate which determines which value to return.
     * @return Whether any matches are present in the object.
     */
    public static <T> boolean anyMatches(final Iterable<T> values, final Predicate<T> by) {
        for (T val : values) {
            if (by.test(val)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Safely retrieves a value from the input map.
     *
     * @param <K> The unused key type in the given map.
     * @param <V> The type of value being retrieved.
     * @param key The key being researched in the map.
     * @return The mapped value, or else {@link Optional#empty}.
     */
    @NotNull
    public static <K, V> Optional<V> getOptional(final Map<K, V> map, final K key) {
        return Optional.ofNullable(map.get(key));
    }

    /**
     * Safely retrieves a value from the input array.
     *
     * @param <T> The type of value being retrieved.
     * @param index The index of the value being returned.
     * @return The value at the given index, or else {@link Optional#empty}.
     */
    @NotNull
    public static <T> Optional<T> getOptional(final T[] array, final int index) {
        return index >= 0 && index < array.length ? nullable(array[index]) : empty();
    }

    /**
     * Variant of Arrays#sort which returns the array.
     *
     * @param array The array of integers being sorted.
     * @return The sorted array.
     */
    @Contract(mutates = "param1")
    public static int[] sort(final int[] array) {
        Arrays.sort(array);
        return array;
    }

    /**
     * Variant of Arrays#sort which returns the array.
     *
     * @param array The array of floats being sorted.
     * @return The sorted array.
     */
    @Contract(mutates = "param1")
    public static float[] sort(final float[] array) {
        Arrays.sort(array);
        return array;
    }

    /**
     * Maps the given list to an {@link ArrayList} of a new type.
     *
     * @param <T> The original type of the map being converted.
     * @param <U> The new type of value in the map.
     * @param list The list being converted.
     * @param mapper A function which converts the individual values.
     * @return A transformed list containing the new type.
     */
    @NotNull
    public static <T, U> List<U> map(final List<T> list, final Function<T, U> mapper) {
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * Maps the given set to an {@link HashSet} of a new type.
     *
     * @param <T> The original type of the map being converted.
     * @param <U> The new type of value in the map.
     * @param set The set being converted.
     * @param mapper A function which converts the individual values.
     * @return A transformed list containing the new type.
     */
    @NotNull
    public static <T, U> Set<U> map(final Set<T> set, final Function<T, U> mapper) {
        return set.stream().map(mapper).collect(Collectors.toSet());
    }

    /**
     * Variant of {@link Collection#removeIf} which returns the data removed.
     *
     * @param source The original data source being copied out of.
     * @param filter The filter for which files to remove from the collection.
     * @param <T> The type of data in the collection.
     * @return A new collection containing the removed data.
     */
    @NotNull
    @Contract(mutates = "param1")
    public static <T> Set<T> drain(final Set<T> source, final Predicate<T> filter) {
        return drain(source, new HashSet<>(), filter);
    }

    /**
     * Variant of {@link Collection#removeIf} which returns the data removed.
     *
     * @param source The original data source being copied out of.
     * @param filter The filter for which files to remove from the collection.
     * @param <T> The type of data in the collection.
     * @return A new collection containing the removed data.
     */
    @NotNull
    @Contract(mutates = "param1")
    public static <T> List<T> drain(final List<T> source, final Predicate<T> filter) {
        return drain(source, new ArrayList<>(), filter);
    }

    /**
     * Variant of {@link Collection#removeIf} which returns the data removed.
     *
     * @param source The original data source being copied out of.
     * @param into The collection being copied into.
     * @param filter The filter for which files to remove from the collection.
     * @param <T> The type of data in the collection.
     * @param <R> The type of collection being returned.
     * @return A new collection containing the removed data.
     */
    @NotNull
    @Contract(mutates = "param1, param2")
    public static <T, R extends Collection<T>> R drain(final Collection<T> source, final R into, final Predicate<T> filter) {
        final Iterator<T> iterator = source.iterator();
        while (iterator.hasNext()) {
            final T item = iterator.next();
            if (filter.test(item)) {
                iterator.remove();
                into.add(item);
            }
        }
        return into;
    }

    /**
     * Direct variant of {@link Stream#filter} which generates a new collection.
     *
     * @param source The original data source being filtered out of.
     * @param filter The filter for which files to retain in the collection.
     * @param <T> The type of data in the collection.
     * @return A new collection containing the filtered data.
     */
    @Contract(pure = true)
    public static <T> List<T> filter(final List<T> source, final Predicate<T> filter) {
        return filter(source, new ArrayList<>(), filter);
    }

    /**
     * Direct variant of {@link Stream#filter} which generates a new collection.
     *
     * @param source The original data source being filtered out of.
     * @param filter The filter for which files to retain in the collection.
     * @param <T> The type of data in the collection.
     * @return A new collection containing the filtered data.
     */
    @Contract(pure = true)
    public static <T> Set<T> filter(final Set<T> source, final Predicate<T> filter) {
        return filter(source, new HashSet<>(), filter);
    }

    /**
     * Direct variant of {@link Stream#filter} which generates a new collection.
     *
     * @param source The original data source being filtered out of.
     * @param into The collection being copied into.
     * @param filter The filter for which files to retain in the collection.
     * @param <T> The type of data in the collection.
     * @param <R> The type of collection being returned.
     * @return A new collection containing the filtered data.
     */
    @Contract(mutates = "param2")
    public static <T, R extends Collection<T>> R filter(final Collection<T> source, final R into, final Predicate<T> filter) {
        for (final T t : source) {
            if (filter.test(t)) {
                into.add(t);
            }
        }
        return into;
    }

    /**
     * Zips two iterators together and iterates through them <b>concurrently</b>.
     *
     * <p>For example, given the following iterators:
     *
     * <ul>
     *   <li><code>['a', 'b', 'c']</code></li>
     *   <li><code>[1, 2, 3]</code></li>
     * </ul>
     *
     * <p>The following tuples will be consumed:
     *
     * <ul>
     *   <li><code>('a', 1)</code></li>
     *   <li><code>('b', 2)</code></li>
     *   <li><code>('c', 3)</code></li>
     * </ul>
     *
     * <p>In the event where either iterator yields more values than the other,
     * <b>the remaining data will be ignored</b>.
     *
     * @param t   The first iterator.
     * @param u   The second iterator.
     * @param fn  The consumer accepting both values.
     * @param <T> The type of the first iterator.
     * @param <U> The type of the second iterator.
     */
    public static <T, U> void biConsume(final Iterable<T> t, final Iterable<U> u, final BiConsumer<T, U> fn) {
        final Iterator<T> tIterator = t.iterator();
        final Iterator<U> uIterator = u.iterator();

        while (tIterator.hasNext() && uIterator.hasNext()) {
            fn.accept(tIterator.next(), uIterator.next());
        }
    }

    /**
     * Chains two iterators and iterates through every possible tuple, in order.
     *
     * <p>For example, when given the following iterators:</p>
     *
     * <ul>
     *   <li><code>['a']</code></li>
     *   <li><code>[1, 2, 3]</code></li>
     * </ul>
     *
     * <p>The following tuples will be consumed:
     *
     * <ul>
     *   <code>('a', 1)</code>
     *   <code>('a', 2)</code>
     *   <code>('a', 3)</code>
     * </ul>
     *
     * @param t   The first iterator.
     * @param u   The second iterator.
     * @param fn  The consumer accepting both values.
     * @param <T> The type of the first iterator.
     * @param <U> The type of the second iterator.
     */
    public static <T, U> void forEachNested(final Iterable<T> t, final Iterable<U> u, final BiConsumer<T, U> fn) {
        for (final T t1 : t) for (final U u1 : u) fn.accept(t1, u1);
    }

    /**
     * Chains three iterators and iterates through every possible tuple, in order.
     *
     * @param t   The first iterator.
     * @param u   The second iterator.
     * @param v   The third iterator.
     * @param fn  The consumer accepting both values.
     * @param <T> The type of the first iterator.
     * @param <U> The type of the second iterator.
     * @param <V> The type of the third iterator.
     */
    public static <T, U, V> void forEachNested(
            final Iterable<T> t, final Iterable<U> u, final Iterable<V> v, final TriConsumer<T, U, V> fn) {
        for (final T t1 : t) for (final U u1 : u) for (final V v1: v) fn.accept(t1, u1, v1);
    }

    /**
     * Retrieves an enum constant by name.
     *
     * @throws InvalidEnumConstantException If the given key is invalid.
     * @param s The name of the constant being researched.
     * @param clazz The enum class which contains the expected constant.
     * @param <T> The type of constant being researched.
     * @return The expected constant.
     */
    @NotNull
    public static <T extends Enum<T>> T assertEnumConstant(final String s, final Class<T> clazz) {
        return getEnumConstant(s, clazz).orElseThrow(() -> invalidConstant(s, clazz));
    }

    /**
     * Retrieves an enum constant by name.
     *
     * @param s The name of the constant being researched.
     * @param clazz The enum class which contains the expected constant.
     * @param <E> The type of constant being researched.
     * @return The expected constant, or else {@link Optional#empty}.
     */
    public static <E extends Enum<E>> Optional<E> getEnumConstant(final String s, final Class<E> clazz) {
        return find(clazz.getEnumConstants(), e -> isFormatted(e, s));
    }

    /**
     * Determines whether a string matches the given enum constant's name, ignoring
     * case and underscores (<code>_</code>).
     *
     * @param e The enum constant being compared.
     * @param s The string identifier for this constant.
     * @param <E> The type of enum value.
     * @return Whether this string is a valid identifier for the constant.
     */
    public static <E extends Enum<E>> boolean isFormatted(final E e, final String s) {
        final String id = e.name().replace("_", "");
        return id.equalsIgnoreCase(s.replace("_", ""));
    }
}
