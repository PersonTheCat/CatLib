package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import xjs.data.comments.CommentStyle;
import xjs.data.serialization.token.TokenType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static personthecat.catlib.command.CommandUtils.displayOnHover;

/**
 * An extensible, modular linting API for displaying formatted data in the chat.
 *
 * <p>This class may be used either as a functional interface or entrypoint
 * to construct a {@link LinterDelegate composed linter} from one or more
 * {@link Highlighter highlighters}.
 */
@FunctionalInterface
public interface Linter {

    /** Singleton indicating a random color was chosen */
    Style RANDOM_COLOR = Colors.STYLE;

    /** Default linter for DJS and JSON-like data */
    Linter DJS = Linter.of(
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
    );

    /**
     * Convert the given text into a {@link Component}.
     *
     * @param text The source text being updated.
     * @return A {@link Component component} with formatting applied.
     */
    Component lint(String text);

    /**
     * Modify the literal text input to this function.
     *
     * @param before A transformation of the input text before linting.
     * @return A new {@link Linter linter} with preprocessing applied.
     */
    default Linter compose(UnaryOperator<String> before) {
        return text -> this.lint(before.apply(text));
    }

    /**
     * Modify the generated {@link Component} returned by this function.
     *
     * @param after A transformation of the output after linting.
     * @return A new {@link Linter linter} with postprocessing applied.
     */
    default Linter andThen(UnaryOperator<Component> after) {
        return text -> after.apply(this.lint(text));
    }

    /**
     * Overrides the default format when this linter does not apply.
     *
     * @param formats Default formatting of the output {@link Component component}.
     * @return A new {@link Linter linter} with background applied.
     */
    default Linter withBackground(ChatFormatting... formats) {
        return this.withBackground(style(formats));
    }

    /**
     * Overrides the default format when this linter does not apply.
     *
     * @param style The default style of the output {@link Component component}.
     * @return A new {@link Linter linter} with background applied.
     */
    default Linter withBackground(Style style) {
        final var rand = new Random(1234); // not supposed to be random, but a sequence
        return this.andThen(c -> Component.empty().withStyle(Colors.get(style, rand.nextInt())));
    }

    /**
     * Produce a {@link Linter} from an array of {@link Highlighter Highlighters}.
     *
     * <pre>{@code
     *   static final Linter TRUE_LINTER =
     *     Linter.compose(
     *        new RegexHighlighter("true", color(ChatFormatting.GOLD))
     *     );
     * }</pre>
     *
     * @param highlighters An array of {@link Highlighter highlighters}.
     * @return A function which generates a {@link Component component} from text.
     */
    static Linter of(Highlighter... highlighters) {
        return of(Arrays.asList(highlighters));
    }

    /**
     * Variant of {@link Linter#of(Highlighter...)} consuming a list instead of
     * an array.
     *
     * @param highlighters An array of {@link Highlighter highlighters}.
     * @return A function which generates a {@link Component component} from text.
     */
    static Linter of(List<Highlighter> highlighters) {
        return new LinterDelegate(highlighters);
    }

    /**
     * Generate a simple {@link Linter linter} from a list of {@link ChatFormatting
     * formats}.
     *
     * @param formats A set of colors and other formatters to produce a {@link Style
     *                style}.
     * @return A function which generates a {@link Component component} from text.
     */
    static Linter from(ChatFormatting... formats) {
        return from(style(formats));
    }

    /**
     * Generate a simple {@link Linter linter} from a specific {@link Style}
     *
     * @param style The exact style of the output {@link Component component}.
     * @return A function which generates a {@link Component component} from text.
     */
    static Linter from(Style style) {
        final var rand = new Random(1234); // not supposed to be random, but a sequence
        return text -> Component.literal(text).withStyle(Colors.get(style, rand.nextInt()));
    }

    /**
     * Shorthand for constructing an error style when provided a raw tooltip.
     *
     * @param s The string to display as a tooltip.
     * @return A new {@link Style} used for displaying error messages.
     */
    static Style error(final String s) {
        return style(ChatFormatting.RED, ChatFormatting.UNDERLINE)
            .withHoverEvent(displayOnHover(Component.translatable(s)));
    }

    /**
     * Returns a new {@link Style} with the given color.
     *
     * @param formats An array of {@link ChatFormatting chat formatting} options.
     * @return A new style with the given color.
     */
    static Style style(final ChatFormatting... formats) {
        return Style.EMPTY.applyFormats(formats);
    }
}
