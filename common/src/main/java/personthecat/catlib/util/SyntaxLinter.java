package personthecat.catlib.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
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
    public static final Highlighter[] HJSON_HIGHLIGHTERS = {
        new RegexHighlighter(MULTILINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new RegexHighlighter(LINE_TODO, color(ChatFormatting.YELLOW)),
        new RegexHighlighter(LINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new RegexHighlighter(MULTILINE_COMMENT, color(ChatFormatting.GRAY)),
        new RegexHighlighter(LINE_COMMENT, color(ChatFormatting.GRAY)),
        new RegexHighlighter(KEY, color(ChatFormatting.AQUA)),
        new RegexHighlighter(BOOLEAN_VALUE, color(ChatFormatting.GOLD)),
        new RegexHighlighter(NUMERIC_VALUE, color(ChatFormatting.LIGHT_PURPLE)),
        new RegexHighlighter(NULL_VALUE, color(ChatFormatting.RED))
    };

    /** The default Hjson syntax linter provided by the library. */
    public static final SyntaxLinter DEFAULT_LINTER = new SyntaxLinter(HJSON_HIGHLIGHTERS);

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

    /** Whichever targets the author has selected for highlighting their text. */
    final Highlighter[] highlighters;

    /**
     * Constructs a new SyntaxLinter used for linting in the standard MC chat.
     *
     * @param highlighters An array of text highlighters.
     */
    public SyntaxLinter(final Highlighter[] highlighters) {
        this.highlighters = highlighters;
    }

    /**
     * Deprecated constructor for backwards compatibility.
     *
     * @deprecated Use {@link SyntaxLinter#SyntaxLinter(Highlighter[])}
     */
    @Deprecated
    public SyntaxLinter(final Target[] targets) {
        this.highlighters = Highlighter.fromTargets(targets);
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
        final Context ctx = new Context(text, this.highlighters);

        Highlighter.Instance h;
        int i = 0;
        int m = 0;
        while ((h = ctx.next(i)) != null) {
            final int start = h.start();
            final int end = h.end();

            if (start - i > 0) {
                // Append unformatted text;
                formatted.append(stc(text.substring(i, start)));
            }
            formatted.append(h.replacement());
            ctx.skipTo(end);
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
    @Deprecated
    public static class Target {
        final Pattern pattern;
        final Style style;

        public Target(final Pattern pattern, final Style style) {
            this.pattern = pattern;
            this.style = style;
        }
    }

    /**
     * This interface represents any object storing instructions for how to
     * highlight a body of text. It does not contain the text, nor should it
     * contain any mutable data for tracking the text. Rather, it should
     * provide an {@link Instance} which does once the text becomes available.
     */
    public interface Highlighter {
        Instance get(String text);

        @Deprecated
        static Highlighter[] fromTargets(final Target[] targets) {
            final Highlighter[] highlighters = new Highlighter[targets.length];
            for (int i = 0; i < targets.length; i++) {
                final Target target = targets[i];
                highlighters[i] = new RegexHighlighter(target.pattern, target.style);
            }
            return highlighters;
        }

        /**
         * This interface represents an object which tracks and applies
         * formatting changes to a body of text. It houses the logic
         * responsible for locating the text and will provide the updated
         * text when it becomes available.
         */
        interface Instance {
            void next();
            boolean found();
            int start();
            int end();
            Component replacement();
        }
    }

    /**
     * A highlighter which applies a single pattern based on a regular expression.
     *
     * <p>Note that any part of the expression matched will be consumed by this
     * highlighter. Use lookaheads and lookbehinds to avoid highlighting matched
     * text, as this particular highlighter will ignore grouping.
     */
    public static class RegexHighlighter implements Highlighter {
        final Pattern pattern;
        final Style style;

        public RegexHighlighter(final Pattern pattern, final Style style) {
            this.pattern = pattern;
            this.style = style;
        }

        @Override
        public Instance get(final String text) {
            return new RegexHighlighterInstance(this.pattern.matcher(text), text, this.style);
        }

        public static class RegexHighlighterInstance implements Instance {
            final Matcher matcher;
            final String text;
            final Style style;
            boolean found;

            public RegexHighlighterInstance(final Matcher matcher, final String text, final Style style) {
                this.matcher = matcher;
                this.text = text;
                this.style = style;
                this.found = matcher.find();
            }

            @Override
            public void next() {
                this.found = this.matcher.find();
            }

            @Override
            public boolean found() {
                return this.found;
            }

            @Override
            public int start() {
                return this.matcher.start();
            }

            @Override
            public int end() {
                return this.matcher.end();
            }

            @Override
            public Component replacement() {
                return new TextComponent(this.text.substring(this.start(), this.end())).setStyle(this.style);
            }
        }
    }

    /**
     * The context used for highlighting text output. Essentially just a list of
     * {@link Matcher} -> {@link Style} for the given text.
     */
    private static class Context {
        final List<Highlighter.Instance> highlighters = new ArrayList<>();
        final String text;

        Context(final String text, final Highlighter[] highlighters) {
            this.text = text;
            for (final Highlighter highlighter : highlighters) {
                this.highlighters.add(highlighter.get(text));
            }
        }

        @Nullable
        Highlighter.Instance next(final int i) {
            // Figure out whether any other matches have been found;
            int start = Integer.MAX_VALUE;
            Highlighter.Instance first = null;
            for (final Highlighter.Instance h : this.highlighters) {
                if (!h.found()) continue;
                final int mStart = h.start();

                if (mStart >= i && mStart < start) {
                    start = mStart;
                    first = h;
                }
            }
            return first;
        }

        void skipTo(final int i) {
            for (final Highlighter.Instance h : this.highlighters) {
                if (!h.found()) continue;
                if (h.end() <= i) {
                    h.next();
                }
            }
        }
    }
}
