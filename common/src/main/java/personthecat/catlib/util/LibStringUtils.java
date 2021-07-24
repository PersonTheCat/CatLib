package personthecat.catlib.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class LibStringUtils {
    public static List<String> wrapLines(final String description, final int l) {
        final List<String> lines = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        final StringBuilder word = new StringBuilder();
        boolean nl = false;

        for (final char c : description.toCharArray()) {
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
