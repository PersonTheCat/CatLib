package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;
import oshi.annotation.concurrent.ThreadSafe;
import xjs.data.comments.CommentStyle;
import xjs.data.serialization.token.TokenType;

import java.util.ArrayList;
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

    /** Singleton indicating a random color was chosen */
    public static final Style RANDOM_COLOR = Style.EMPTY.applyFormats();

    /** Default configuration for DJS and JSON-like data */
    public static final Highlighter[] COMMON_HIGHLIGHTERS = {
        TokenHighlighter.builder()
            .key(ChatFormatting.LIGHT_PURPLE)
            .token(TokenType.COMMENT, ChatFormatting.GRAY)
            .comment(CommentStyle.MULTILINE_DOC, ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC)
            .comment(CommentStyle.LINE_DOC, ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC)
            .comment(Pattern.compile("^\\s*(todo|to-do)", Pattern.CASE_INSENSITIVE), ChatFormatting.YELLOW)
            .word(Pattern.compile("^(?:true|false)$"), ChatFormatting.GOLD)
            .word(Pattern.compile("^null$"), ChatFormatting.RED)
            .token(TokenType.NUMBER, ChatFormatting.AQUA)
            .token(TokenType.STRING, ChatFormatting.DARK_GREEN)
            .build(),
        UnbalancedTokenHighlighter.INSTANCE
    };

    /** Default linter for DJS and JSON-like data */
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
     * Gets the suggested style or a random style by index
     *
     * @param style The style requested by the user / highlighter configuration
     * @param idx   <em>Any</em> index used by the highlighter
     * @return Either the suggested style or a random style by index
     */
    static Style checkStyle(final Style style, int idx) {
        return style != RANDOM_COLOR ? style : RANDOM_COLORS[idx % RANDOM_COLORS.length];
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
