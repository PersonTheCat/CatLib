package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.exception.InvalidEnumConstantException;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static personthecat.catlib.exception.Exceptions.invalidConstant;

/**
 * A collection of platform-agnostic <b>shorthand</b> utilities.
 */
@UtilityClass
public class LibUtil {

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
     * Casts a subtype to a parent type.
     *
     * @param type The actual class type.
     * @param <T>  The type of the class.
     * @return The parent class.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> asParentType(final Class<? super T> type) {
        return (Class<T>) type;
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
     * Safely retrieves a value from the input map.
     *
     * @param <K> The unused key type in the given map.
     * @param <V> The type of value being retrieved.
     * @param map The map being queried from.
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
     * @param array The array containing the expected value.
     * @param index The index of the value being returned.
     * @return The value at the given index, or else {@link Optional#empty}.
     */
    @NotNull
    public static <T> Optional<T> getOptional(final T[] array, final int index) {
        return index >= 0 && index < array.length ? Optional.ofNullable(array[index]) : empty();
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
        return Stream.of(clazz.getEnumConstants()).filter(e -> isFormatted(e, s)).findFirst();
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
        final String id = e.name().replace("_", "").replace(" ", "");
        return id.equalsIgnoreCase(s.replace("_", "").replace(" ", ""));
    }
}
