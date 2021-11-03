package personthecat.catlib.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Contains any utilities needed by the library for manipulating miscellaneous
 * string values. Currently provides a single utility for wrapping text onto
 * multiple lines, but additional methods may be provided at some point in the
 * future.
 */
@UtilityClass
public class LibStringUtils {

    private static final Random RAND = new Random();

    /**
     * Wraps the given text onto multiple lines when given a line length.
     *
     * @param text The string value being manipulated.
     * @param l The maximum number of characters per line.
     * @return The wrapped lines as a list of strings.
     */
    public static List<String> wrapLines(final String text, final int l) {
        final List<String> lines = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        final StringBuilder word = new StringBuilder();
        boolean nl = false;

        for (final char c : text.toCharArray()) {
            if (c == '\r') continue;
            if (c == '\n') { nl = true; continue; }
            if (c == ' ' || c == '\t' || nl) {
                if (word.length() + line.length() > l) {
                    lines.add(line.toString());
                    line.setLength(0);
                    word.append(' ');
                } else {
                    line.append(word).append(' ');
                    word.setLength(0);
                }
            } else {
                word.append(c);
            }
            nl = false;
        }
        if (word.length() + line.length() > l) {
            lines.add(line.toString());
            lines.add(word.toString());
        } else {
            line.append(word);
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * Generates a random string of characters (a ~ z) of the given length.
     *
     * @param length The number of characters in the string.
     * @return A new string random characters.
     */
    public static String randId(final int length) {
        final char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) (RAND.nextInt(26) + 'a');
        }
        return new String(chars);
    }

    /**
     * Converts a string in lower case or snake case to title case.
     *
     * <p>Note that <b>camel case is not supported</b>. Please submit
     * a PR if you would like this functionality.
     *
     * @param text The text in lower case or snake case.
     * @return The equivalent message in title case.
     */
    public static String toTitleCase(final String text) {
        if (text.isEmpty()) return "";

        final StringBuilder sb = new StringBuilder(text.length());
        boolean capitalize = false;

        sb.append(Character.toUpperCase(text.charAt(0)));

        for (int i = 1; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == ' ' || c == '_' || c == '-') {
                capitalize = true;
                sb.append(' ');
            } else if (capitalize) {
                sb.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
