package personthecat.catlib.util;

import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A bare-bones syntax linter for displaying some formatted data in the chat.
 * <p>
 *   This class is <em>not intended</em> to be a foolproof utility. It is only
 *   designed for a few scenarios and can highlight keys and documentation.
 * </p>
 * <p>
 *   Eventually, this class will be expanded to support matching individual
 *   groups in an expression, but this behavior is <b>currently unsupported</b>.
 * </p>
 */
@SuppressWarnings("unused")
public class SyntaxLinter {

    /** Identifies multiline documentation comments. Just because. */
    public static final Pattern MULTILINE_DOC = Pattern.compile("/\\*\\*[\\s\\S]*?\\*/", Pattern.DOTALL);

    /** Identifies multiline / inline comments to be highlighted. */
    public static final Pattern MULTILINE_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/", Pattern.DOTALL);

    /** Identifies todos in single line comments. Just because. */
    public static final Pattern LINE_TODO = Pattern.compile("(?:#|//).*(?:todo|to-do).*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    /** Identifies single line documentation comments. Just because. */
    public static final Pattern LINE_DOC = Pattern.compile("(?:#!|///).*$", Pattern.MULTILINE);

    /** Identifies single line comments to be highlighted. */
    public static final Pattern LINE_COMMENT = Pattern.compile("(?:#|//).*$", Pattern.MULTILINE);

    /** Identifies all other Hjson / Yaml keys to be highlighted. */
    public static final Pattern KEY = Pattern.compile("(\"[\\w\\s]*\"|\\w+)\\s*(?=:)|[-_\\w./]+\\s*(?:::|[aA][sS])\\s*\\w+(.*\\s[aA][sS]\\s+\\w+)?", Pattern.MULTILINE);

    /** Identifies all boolean values to be highlighted. */
    public static final Pattern BOOLEAN_VALUE = Pattern.compile("(true|false)(?=\\s*,?\\s*(?:$|#|//|/\\*))", Pattern.MULTILINE);

    /** Identifies all numeric values to be highlighted. */
    public static final Pattern NUMERIC_VALUE = Pattern.compile("(\\d+(\\.\\d+)?)(?=\\s*,?\\s*(?:$|#|//|/\\*))", Pattern.MULTILINE);

    /** Identifies all null values to be highlighted. */
    public static final Pattern NULL_VALUE = Pattern.compile("(null)(?=\\s*,?\\s*(?:$|#|//|/\\*))", Pattern.MULTILINE);

    /** Indicates that a random color should be used for each match of this type. */
    protected static final Style RANDOM_COLOR = null;

    /** A list of every target needed for highlighting basic Hjson data. */
    public static final Target[] HJSON_TARGETS = {
        new Target(MULTILINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new Target(LINE_TODO, color(ChatFormatting.YELLOW)),
        new Target(LINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new Target(MULTILINE_COMMENT, color(ChatFormatting.GRAY)),
        new Target(LINE_COMMENT, color(ChatFormatting.GRAY)),
        new Target(KEY, color(ChatFormatting.AQUA)),
        new Target(BOOLEAN_VALUE, color(ChatFormatting.GOLD)),
        new Target(NUMERIC_VALUE, color(ChatFormatting.LIGHT_PURPLE)),
        new Target(NULL_VALUE, color(ChatFormatting.RED))
    };

    /** The default Hjson syntax linter provided by the library. */
    public static final SyntaxLinter DEFAULT_LINTER = new SyntaxLinter(HJSON_TARGETS);

    /** An array of colors to be used in place of the wildcard color. */
    private static final Style[] RANDOM_COLORS = {
        color(ChatFormatting.YELLOW),
        color(ChatFormatting.GREEN),
        color(ChatFormatting.AQUA),
        color(ChatFormatting.GOLD),
        color(ChatFormatting.BLUE),
        color(ChatFormatting.LIGHT_PURPLE),
        color(ChatFormatting.RED)
    };

    /** Whichever targets the author has selected for highlighting their text.s */
    final Target[] targets;

    /**
     * Constructs a new SyntaxHighlighter used for linting in the standard MC chat.
     *
     * @param targets An array of {@link Pattern} -> {@link Style}.
     */
    public SyntaxLinter(final Target[] targets) {
        this.targets = targets;
    }

    /**
     * Generates a formatted text component containing the input text the requested
     * highlights.
     *
     * @param text Any text of the expected syntax.
     * @return A linted text output containing the original message.
     */
    public Component lint(final String text) {
        final TextComponent formatted = new TextComponent("");
        final Context ctx = new Context(text, this.targets);

        Scope s;
        int i = 0;
        int m = 0;
        while ((s = ctx.next(i)) != null) {
            final int start = s.matcher.start();
            final int end = s.matcher.end();
            ctx.skipTo(end);

            if (start - i > 0) {
                // Append unformatted text;
                formatted.append(stc(text.substring(i, start)));
            }
            formatted.append(stc(text.substring(start, end)).setStyle(getStyle(s.style, m++)));

            i = end;
        }

        return formatted.append(stc(text.substring(i)));
    }

    /**
     * Shorthand for a regular, formattable {@link TextComponent}.
     *
     * @param s The text being wrapped.
     * @return A wrapped, formattable output containing the original message.
     */
    public static TextComponent stc(final String s) {
        return new TextComponent(s);
    }

    /**
     * Returns a new {@link Style} with the given color.
     *
     * @param color The {@link ChatFormatting} color.
     * @return A new style with the given color.
     */
    public static Style color(final ChatFormatting color) {
        return Style.EMPTY.withColor(color);
    }

    /**
     * Generates a random color for use with the current match, if applicable.
     *
     * @param style The style configured for use with this target.
     * @param m     The number of the current match.
     * @return The input style, or else a random style, if null.
     */
    private static Style getStyle(@Nullable final Style style, int m) {
        return style != null ? style : RANDOM_COLORS[m % RANDOM_COLORS.length];
    }

    /**
     * A map of {@link Pattern} -> {@link Style}.
     */
    @AllArgsConstructor
    public static class Target {
        final Pattern pattern;
        final Style style;
    }

    /**
     * The context used for highlighting text output. Essentially just a list of
     * {@link Matcher} -> {@link Style} for the given text.
     */
    private static class Context {
        final List<Scope> scopes = new ArrayList<>();
        final String text;

        Context(final String text, final Target[] targets) {
            this.text = text;
            for (final Target t : targets) {
                final Matcher matcher = t.pattern.matcher(text);
                this.scopes.add(new Scope(matcher, t.style, matcher.find()));
            }
        }

        @Nullable
        Scope next(final int i) {
            // Figure out whether any other matches have been found;
            int start = Integer.MAX_VALUE;
            Scope first = null;
            for (final Scope s : this.scopes) {
                if (!s.found) continue;
                final int mStart = s.matcher.start();

                if (mStart >= i && mStart < start) {
                    start = mStart;
                    first = s;
                }
            }
            return first;
        }

        void skipTo(final int i) {
            for (final Scope s : this.scopes) {
                if (!s.found) continue;
                if (s.matcher.end() <= i) {
                    s.next();
                }
            }
        }
    }

    @AllArgsConstructor
    private static class Scope {
        final Matcher matcher;
        final Style style;
        boolean found;

        void next() {
            this.found = this.matcher.find();
        }
    }
}
