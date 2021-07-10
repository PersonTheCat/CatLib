package personthecat.catlib.util;

import lombok.experimental.UtilityClass;

import java.util.Optional;

/**
 * A collection of platform-agnostic <b>shorthand</b> utilities.
 */
@UtilityClass
@SuppressWarnings("unused")
public class Sh {

    /** Shorthand for calling Optional#empty. */
    public static <T> Optional<T> empty() {
        return Optional.empty();
    }

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
}
