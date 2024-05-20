package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.Nullable;
import oshi.annotation.concurrent.ThreadSafe;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An extensible, modular linter for displaying formatted data in the chat.
 *
 * <p>To use this class, create a new instance by passing in an array of
 * {@link Highlighter highlighters}. A highlighter is an object which locates
 * and stylizes text. For example, the {@link RegexHighlighter} is capable
 * of matching entire patterns or groups in a regular expression and applying
 * a constant style to the individual matches.
 *
 * <pre>{@code
 *   static final SyntaxLinter TRUE_LINTER =
 *     new SyntaxLinter({
 *        new RegexHighlighter("true", color(ChatFormatting.GOLD))
 *     });
 * }</pre>
 *
 * <p>This object can then produce a formatted {@link Component} when provided
 * a body of text.
 *
 * <p>Note that this object is valid and safe in a multithreaded context.
 */
@ThreadSafe
public class SyntaxLinter {

    public static final Pattern MULTILINE_DOC = Pattern.compile("/\\*\\*[\\s\\S]*?\\*/", Pattern.DOTALL);
    public static final Pattern MULTILINE_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/", Pattern.DOTALL);
    public static final Pattern LINE_TODO = Pattern.compile("(?:#|//).*(?:todo|to-do).*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    public static final Pattern LINE_DOC = Pattern.compile("(?:#!|///).*$", Pattern.MULTILINE);
    public static final Pattern LINE_COMMENT = Pattern.compile("(?:#|//).*$", Pattern.MULTILINE);
    public static final Pattern KEY = Pattern.compile("(\"[\\w\\s]*\"|\\w+)\\s*(?=:)|[-_\\w./]+\\s*(?:::|[aA][sS])\\s*\\w+(.*\\s[aA][sS]\\s+\\w+)?", Pattern.MULTILINE);
    public static final Pattern BOOLEAN_VALUE = Pattern.compile("(true|false)(?=\\s*,?\\s*(?:$|#|//|/\\*))", Pattern.MULTILINE);
    public static final Pattern NUMERIC_VALUE = Pattern.compile("(\\d+(\\.\\d+)?)(?=\\s*,?\\s*(?:$|#|//|/\\*))", Pattern.MULTILINE);
    public static final Pattern NULL_VALUE = Pattern.compile("(null)(?=\\s*,?\\s*(?:$|#|//|/\\*))", Pattern.MULTILINE);
    public static final Pattern BAD_CLOSER = Pattern.compile("[a-zA-Z]\\w*(?<!true|false|null)[\\t ]*[]}]", Pattern.MULTILINE);

    protected static final Style UNCLOSED_ERROR = error("catlib.errorText.unclosed");
    protected static final Style UNEXPECTED_ERROR = error("catlib.errorText.unclosed");
    protected static final Style BAD_CLOSER_ERROR = error("catlib.errorText.badCloser");

    protected static final Style RANDOM_COLOR = null;

    public static final Highlighter[] COMMON_HIGHLIGHTERS = {
        new RegexHighlighter(MULTILINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new RegexHighlighter(LINE_TODO, color(ChatFormatting.YELLOW)),
        new RegexHighlighter(LINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new RegexHighlighter(MULTILINE_COMMENT, color(ChatFormatting.GRAY)),
        new RegexHighlighter(LINE_COMMENT, color(ChatFormatting.GRAY)),
        new RegexHighlighter(KEY, color(ChatFormatting.AQUA)),
        new RegexHighlighter(BOOLEAN_VALUE, color(ChatFormatting.GOLD)),
        new RegexHighlighter(NUMERIC_VALUE, color(ChatFormatting.LIGHT_PURPLE)),
        new RegexHighlighter(NULL_VALUE, color(ChatFormatting.RED)),
        new RegexHighlighter(BAD_CLOSER, BAD_CLOSER_ERROR),
        UnbalancedTokenHighlighter.INSTANCE
    };

    public static final SyntaxLinter DEFAULT_LINTER = new SyntaxLinter(COMMON_HIGHLIGHTERS);

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
     * Generates a formatted text component containing the input text the requested
     * highlights.
     *
     * @param text Any text of the expected syntax.
     * @return A linted text output containing the original message.
     */
    public Component lint(final String text) {
        final MutableComponent formatted = Component.empty();
        final Context ctx = new Context(text, this.highlighters);

        Highlighter.Instance h;
        int i = 0;
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
     * Shorthand for constructing an error style when provided a raw tooltip.
     *
     * @param s The string to display as a tooltip.
     * @return A new {@link Style} used for displaying error messages.
     */
    public static Style error(final String s) {
        return Style.EMPTY.applyFormats(ChatFormatting.RED, ChatFormatting.UNDERLINE)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, translate(s)));
    }

    /**
     * Shorthand for a regular, formattable {@link Component} with {@link PlainTextContents}.
     *
     * @param s The text being wrapped.
     * @return A wrapped, formattable output containing the original message.
     */
    public static MutableComponent stc(final String s) {
        return Component.literal(s);
    }

    /**
     * Shorthand for a {@link Component} with {@link TranslatableContents}.
     *
     * @param s The text being wrapped.
     * @return A wrapped, formattable output containing the translated message.
     */
    public static MutableComponent translate(final String s) {
        return Component.translatable(s);
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
     * This interface represents any object storing instructions for how to
     * highlight a body of text. It does not contain the text, nor should it
     * contain any mutable data for tracking the text. Rather, it should
     * provide an {@link Instance} which does once the text becomes available.
     */
    public interface Highlighter {
        Instance get(String text);

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
     * <p>Note that, by default, any part of the expression matched will be consumed
     * by this highlighter. To explicitly match the individual groups within a pattern,
     * apply the <code>useGroups</code> flag to the constructor.
     */
    public static class RegexHighlighter implements Highlighter {
        final Pattern pattern;
        final Style style;
        final boolean useGroups;

        public RegexHighlighter(final @RegExp String pattern, final Style style) {
            this(Pattern.compile(pattern, Pattern.MULTILINE), style);
        }

        public RegexHighlighter(final Pattern pattern, final Style style) {
            this(pattern, style, false);
        }

        public RegexHighlighter(final Pattern pattern, final Style style, final boolean useGroups) {
            this.pattern = pattern;
            this.style = style;
            this.useGroups = useGroups;
        }

        @Override
        public Instance get(final String text) {
            return new RegexHighlighterInstance(text);
        }

        public class RegexHighlighterInstance implements Instance {
            final Matcher matcher;
            final String text;
            final int groups;
            boolean found;
            int group;
            int m;

            public RegexHighlighterInstance(final String text) {
                this.matcher = pattern.matcher(text);
                this.text = text;
                this.groups = useGroups ? matcher.groupCount() : 0;
                this.found = matcher.find();
                this.group = found && groups > 0 ? 1 : 0;
                this.m = 0;
            }

            @Override
            public void next() {
                if (this.group < this.groups) {
                    this.group++;
                } else {
                    this.found = this.matcher.find();
                    if (this.found) {
                        this.m++;
                    }
                }
            }

            @Override
            public boolean found() {
                return this.found;
            }

            @Override
            public int start() {
                return this.matcher.start(this.group);
            }

            @Override
            public int end() {
                return this.matcher.end(this.group);
            }

            @Override
            public Component replacement() {
                return Component.literal(this.text.substring(this.start(), this.end())).setStyle(this.getStyle());
            }

            private Style getStyle() {
                return style != null ? style : RANDOM_COLORS[this.m % RANDOM_COLORS.length];
            }
        }
    }

    /**
     * A simple highlighter used for locating any unclosed or unexpected container
     * elements. These characters will be highlighted in red and underlined with a
     * tooltip displaying details about the issue.
     *
     * <p>Note that this highlighter is not designed for high accuracy. It is not
     * aware that container elements will be consumed by raw, unquoted strings.
     * Instead, it assumes that these errors are unrelated and expects that they
     * will be highlighted by some other object. This is an intentionally pragmatic
     * approach, but may need more testing.
     *
     * <p>Please share any feedback regarding this highlighter to PersonTheCat on
     * the GitHub repository for this project.
     */
    public static class UnbalancedTokenHighlighter implements Highlighter {

        public static final UnbalancedTokenHighlighter INSTANCE = new UnbalancedTokenHighlighter();

        private UnbalancedTokenHighlighter() {}

        @Override
        public Instance get(final String text) {
            return new UnbalancedTokenHighlighterInstance(text);
        }

        public static class UnbalancedTokenHighlighterInstance implements Instance {
            final BitSet unclosed = new BitSet();
            final BitSet unexpected = new BitSet();
            int unclosedIndex = 0;
            int unexpectedIndex = 0;
            final String text;

            public UnbalancedTokenHighlighterInstance(final String text) {
                this.text = text;
                this.resolveErrors();
                this.next();
            }

            private void resolveErrors() {
                boolean esc = false;
                int braces = 0;
                int brackets = 0;
                int i = 0;
                while (i < this.text.length()) {
                    if (esc) {
                        esc = false;
                        i++;
                        continue;
                    }
                    switch (this.text.charAt(i)) {
                        case '\\' -> esc = true;
                        case '{' -> {
                            if (this.findClosing(i, braces, '{', '}') < 0) {
                                this.unclosed.set(i);
                            }
                            braces++;
                        }
                        case '[' -> {
                            if (this.findClosing(i, brackets, '[', ']') < 0) {
                                this.unclosed.set(i);
                            }
                            brackets++;
                        }
                        case '}' -> {
                            if (braces <= 0) {
                                this.unexpected.set(i);
                            }
                            braces--;
                        }
                        case ']' -> {
                            if (brackets <= 0) {
                                this.unexpected.set(i);
                            }
                            brackets--;
                        }
                    }
                    i++;
                }
            }

            private int findClosing(int opening, int ongoing, char open, char close) {
                int numP = ongoing;
                boolean dq = false;
                boolean sq = false;
                boolean esc = false;
                for (int i = opening; i < this.text.length(); i++) {
                    if (esc) {
                        esc = false;
                        continue;
                    }
                    final char c = this.text.charAt(i);
                    if (c == '\\') {
                        esc = true;
                    } else if (c == '"') {
                        dq = !dq;
                    } else if (c == '\'') {
                        sq = !sq;
                    } else if (c == open) {
                        if (!dq && !sq) numP++;
                    } else if (c == close) {
                        if (!dq && !sq && --numP == 0) return i;
                    }
                }
                return -1;
            }

            @Override
            public void next() {
                if (this.unclosedIndex >= 0) {
                    this.unclosedIndex = this.unclosed.nextSetBit(this.unclosedIndex + 1);
                }
                if (this.unexpectedIndex >= 0) {
                    this.unexpectedIndex = this.unexpected.nextSetBit(this.unexpectedIndex + 1);
                }
            }

            @Override
            public boolean found() {
                return this.unclosedIndex >= 0 || this.unexpectedIndex >= 0;
            }

            @Override
            public int start() {
                if (this.unexpectedIndex < 0) {
                    return this.unclosedIndex;
                } else if (this.unclosedIndex < 0) {
                    return this.unexpectedIndex;
                }
                return Math.min(this.unclosedIndex, this.unexpectedIndex);
            }

            @Override
            public int end() {
                return this.start() + 1;
            }

            @Override
            public Component replacement() {
                final int start = this.start();
                final char c = this.text.charAt(start);
                final Style style = start == this.unclosedIndex ? UNCLOSED_ERROR : UNEXPECTED_ERROR;
                return stc(String.valueOf(c)).withStyle(style);
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
