package personthecat.catlib.util;

import personthecat.catlib.data.TextCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Contains any utilities needed by the library for manipulating miscellaneous
 * string values. Currently, provides a single utility for wrapping text onto
 * multiple lines, but additional methods may be provided at some point in the
 * future.
 */
public final class LibStringUtils {

    private static final Random RAND = new Random();

    private LibStringUtils() {}

    /**
     * Wraps the given text onto multiple lines when given a line length.
     *
     * @param text The string value being manipulated.
     * @param l The maximum number of characters per line.
     * @return The wrapped lines as a list of strings.
     */
    public static List<String> wrapLines(final String text, final int l) {
        if (text.length() < l) return Collections.singletonList(text);

        final String normalized = text.replaceAll("\\s+", " ");
        final List<String> lines = new ArrayList<>();
        int index = 0;

        while (index < normalized.length()) {
            lines.add(normalized.substring(index, index = getEndOfLine(normalized, index, index + l)));
            index++;
        }
        return lines;
    }

    /**
     * Returns the closest index of a whitespace character, iterating backwards
     * from <code>index</code>.
     *
     * @param text  The normalized body of text, containing only single spaces.
     * @param last  The last index that was matched by the wrapper.
     * @param index The last possible index when given a maximum line length.
     * @return The last index of this line.
     */
    private static int getEndOfLine(final String text, final int last, final int index) {
        if (index > text.length()) {
            return text.length();
        }
        int i = index;
        while (--i > last) {
            if (text.charAt(i) == ' ') {
                return i;
            }
        }
        // Word is longer than the line. Get the whole word.
        i = index;
        while (++i < text.length()) {
            if (text.charAt(i) == ' ') {
                return i;
            }
        }
        return text.length();
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
     * <p>Note that this method <b>does not support camel or pascal</b>
     * case by default. You must provide an additional argument to
     * optionally support this feature.
     *
     * @param text The text in lower case or snake case.
     * @return The equivalent message in title case.
     */
    public static String toTitleCase(final String text) {
        return toTitleCase(text, false);
    }

    /**
     * Converts a string in lower, snake, camel, or pascal case to title case.
     *
     * @param text The text in lower case or snake case.
     * @param camel Whether to explicitly support camel and or pascal case.
     * @return The equivalent message in title case.
     */
    public static String toTitleCase(final String text, final boolean camel) {
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
            } else if (camel && Character.isUpperCase(c)) {
                sb.append(' ').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Converts a string in camel or pascal case to snake case.
     *
     * @param camel The text in camel or pascal case.
     * @param screaming Whether to output as screaming snake case.
     * @return The equivalent message in snake case.
     */
    public static String toSnakeCase(final String camel, final boolean screaming) {
        if (camel.isEmpty()) return camel;

        final var sb = new StringBuilder();
        for (final var token : tokenize(camel)) {
            if (!sb.isEmpty()) sb.append('_');
            sb.append(screaming ? token.toUpperCase() : token.toLowerCase());
        }
        return sb.toString();
    }

    /**
     * Converts a <b>single</b> word to capital case.
     *
     * @param word The single word of text in any case.
     * @return The same word in capital case.
     */
    public static String capitalize(final String word) {
        if (word.isEmpty()) return "";
        if (word.length() == 1) return String.valueOf(Character.toUpperCase(word.charAt(0)));
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /**
     * Converts a string in camel or pascal case to another case.
     *
     * @param camel The text in camel or pascal case.
     * @param c The target case to convert to.
     * @return The equivalent message in the requested case.
     */
    public static String convertFromCamel(final String camel, final TextCase c) {
        return switch (c) {
            case TITLE -> toTitleCase(camel, true);
            case SNAKE -> toSnakeCase(camel, false);
            case SCREAMING_SNAKE -> toSnakeCase(camel, true);
            case CAMEL, GIVEN -> camel;
            case PASCAL -> capitalize(camel);
        };
    }

    /**
     * Separates a continuous string of alphanumeric characters into an array
     * of words.
     *
     * <p>
     *   For example, the following input:
     * </p><code>
     *   oneTwoThree
     * </code>
     * <p>
     *   Will be converted to this array:
     * </p><code>
     *   [ "one", "Two", "Three" ]
     * </code>
     *
     * @param camel The input text in either camel or pascal case.
     * @return An array of words extracted from the text.
     */
    public static List<String> tokenize(final String camel) {
        if (camel.isEmpty()) return Collections.emptyList();
        final List<String> tokens = new ArrayList<>();
        int index = 0;
        while (index < camel.length()) {
            tokens.add(camel.substring(index, index = getNextWord(camel, index)));
        }
        return tokens;
    }

    /**
     * Returns the next index of a <em>word</em> in the given text.
     *
     * <p>Note that the given text is assumed to contain no whitespace.
     * For this reason, the algorithm may not be ideal for the majority
     * of use cases and thus is not exposed here.
     *
     * @param camel The input text in camel or pascal case.
     * @param index The starting index to begin searching from.
     * @return The next word, or else the end of the text.
     */
    private static int getNextWord(final String camel, int index) {
        char c = camel.charAt(index);
        int numCapital = 0;
        while (Character.isUpperCase(c) && index < camel.length() - 1) {
            c = camel.charAt(++index);
            numCapital++;
        }
        // The last character in abbreviations is usually a new word
        if (numCapital > 1) {
            index--;
        } else {
            while (Character.isLowerCase(c) && index < camel.length() - 1) {
                c = camel.charAt(++index);
            }
        }
        // Get exclusive range for last word.
        return index == camel.length() - 1 ? camel.length() : index;
    }

}
