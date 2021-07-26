package personthecat.catlib.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains any utilities needed by the library for manipulating miscellaneous
 * string values. Currently provides a single utility for wrapping text onto
 * multiple lines, but additional methods may be provided at some point in the
 * future.
 */
@UtilityClass
public class LibStringUtils {

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
}
