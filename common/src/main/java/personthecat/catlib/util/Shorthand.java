package personthecat.catlib.util;

import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static personthecat.catlib.exception.Exceptions.invalidConstant;

/**
 * A collection of platform-agnostic <b>shorthand</b> utilities.
 */
@UtilityClass
@SuppressWarnings("unused")
public class Shorthand {

    /**
     * Shorthand for calling Optional#of, matching the existing syntax of
     * `empty`, while being more clear than `of` alone.
     *
     * @param val The value being wrapped.
     * @param <T> The type of value being wrapped.
     * @return <code>val</code>, wrapped in {@link Optional}.
     */
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
    public static <T> Optional<T> nullable(final T val) {
        return Optional.ofNullable(val);
    }

    /** Returns a random number between the input bounds, inclusive. */
    public static int numBetween(final Random rand, final int min, final int max) {
        return min == max ? min : rand.nextInt(max - min + 1) + min;
    }

    /** Returns a random number between the input bounds, inclusive. */
    public static float numBetween(final Random rand, final float min, final float max) {
        return min == max ? min : rand.nextFloat() * (max - min) + min;
    }

    /**
     * Interpolates strings by replacing instances of <code>{}</code> in order.
     *
     * @param s The template string being formatted.
     * @param args A list of arguments to interpolate into this string.
     * @return A formatted, interpolated string.
     */
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
     * <pre>
     *    // Find x by x.name
     *    Object[] vars = getObjectsWithNames();
     *    Optional<Object> var = find(vars, (x) -> x.name.equals("Cat"));
     *    // You can then get the value -> NPE
     *    Object result = var.get()
     *    // Or use an alternative. Standard java.util.Optional. -> no NPE
     *    Object result = var.orElse(new Object("Cat"))
     * </pre>
     *
     * @param <T> The type of array being passed in.
     * @param values The actual array containing the value.
     * @param by A predicate which determines which value to return.
     * @return The value, or else {@link Optional#empty}.
     */
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
    public static <T> Optional<T> find(final Iterable<T> values, final Predicate<T> by) {
        for (final T val : values) {
            if (by.test(val)) {
                return full(val);
            }
        }
        return empty();
    }

    public static <K, V> Optional<V> find(final Map<K, V> map, final Predicate<V> by) {
        return find(map.values(), by);
    }

    public static <T> List<T> findAll(final Collection<T> values, final Predicate<T> by) {
        final List<T> all = new ArrayList<>();
        for (T val : values) {
            if (by.test(val)) {
                all.add(val);
            }
        }
        return all;
    }

    public static <K, V> List<V> findAll(final Map<K, V> map, final Predicate<V> by) {
        return findAll(map.values(), by);
    }

    /** Determines whether any value in the collection matches the predicate. */
    public static <T> boolean anyMatches(final Collection<T> values, final Predicate<T> by) {
        for (T val : values) {
            if (by.test(val)) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean anyMatches(final T[] values, final Predicate<T> by) {
        for (T val : values) {
            if (by.test(val)) {
                return true;
            }
        }
        return false;
    }

    /** Safely retrieves a value from the input map. */
    public static <K, V> Optional<V> safeGet(final Map<K, V> map, final K key) {
        return Optional.ofNullable(map.get(key));
    }

    /** Safely retrieves a value from the input array. */
    public static <T> Optional<T> safeGet(final T[] array, final int index) {
        return index >= 0 && index < array.length ? full(array[index]) : empty();
    }

    /** Variant of Arrays#sort which returns the array. */
    public static int[] sort(final int[] array) {
        Arrays.sort(array);
        return array;
    }

    /** Variant of Arrays#sort which returns the array. */
    public static float[] sort(final float[] array) {
        Arrays.sort(array);
        return array;
    }

    /** Maps the given list to an {@link ArrayList} of a new type. */
    public static <T, U> List<U> map(final List<T> list, final Function<T, U> mapper) {
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    public static <T extends Enum<T>> T getEnumConstant(final String s, final Class<T> clazz) {
        return find(clazz.getEnumConstants(), v -> v.toString().equalsIgnoreCase(s))
            .orElseThrow(() -> invalidConstant(s, clazz));
    }
}
