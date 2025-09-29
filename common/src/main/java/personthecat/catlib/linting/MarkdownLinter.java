package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static personthecat.catlib.command.CommandUtils.clickToGo;
import static personthecat.catlib.command.CommandUtils.displayOnHover;

public final class MarkdownLinter {
    private static final Pattern C_PREFIX = Pattern.compile("\\s*(?:/{2,}|#|/\\*{1,2}|\\*)?\\s?", Pattern.MULTILINE);
    private static final Pattern BOLD = Pattern.compile("(?<left>\\*{2}|_{2})(?<text>.+?)(?<right>\\1)", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern ITALIC_AST = Pattern.compile("(?<left>(?<!\\*)\\*(?!\\*))(?=\\S)(?<text>.+?)(?<=\\S)(?<right>(?<!\\*)\\*(?!\\*))", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern ITALIC_UND = Pattern.compile("(?<left>(?<!_)_(?!_))(?=\\S)(?<text>.+?)(?<=\\S)(?<right>(?<!_)_(?!_))", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern LINK = Pattern.compile("(?<lb>\\[)(?<text>.*)(?<rb>])(?<lp>\\()(?<link>.*)(?<rp>\\))", Pattern.MULTILINE);
    private static final Pattern FOR_EACH_LINE = Pattern.compile("^" + C_PREFIX + "(?<line>.*)", Pattern.MULTILINE);
    private static final Pattern LIKELY_PARAGRAPH = Pattern.compile("^(?![ \\t]*(#|[-*+][ \\t]|>[ \\t]|\\d+\\.[ \\t]|`{3,})).*$", Pattern.MULTILINE);

    public static final Linter DISPLAY = createForMode(Mode.DISPLAY);
    public static final Linter IN_COMMENTS = createForMode(Mode.IN_COMMENTS);
    public static final Linter DEFAULT = createForMode(Mode.DEFAULT);

    private MarkdownLinter() {}

    private static Linter createForMode(final Mode mode) {
        final var linter = Linter.of(
            headerLayer(mode, 1, 1, ChatFormatting.UNDERLINE, headerColor(mode), ChatFormatting.BOLD),
            headerLayer(mode, 2, 2, headerColor(mode), ChatFormatting.BOLD),
            headerLayer(mode, 3, 6, ChatFormatting.BOLD),
            multilineLayer(mode, BOLD, ChatFormatting.BOLD),
            multilineLayer(mode, ITALIC_AST, ChatFormatting.ITALIC),
            multilineLayer(mode, ITALIC_UND, ChatFormatting.ITALIC),
            codeBlock(mode),
            link(mode),
            unorderedList(mode)
        );
        if (mode == Mode.DISPLAY) {
            return linter.compose(MarkdownLinter::wrapParagraphs);
        }
        return linter;
    }

    private static ChatFormatting headerColor(final Mode mode) {
        return mode == Mode.IN_COMMENTS ? ChatFormatting.GREEN : ChatFormatting.AQUA;
    }

    private static Highlighter headerLayer(final Mode mode, final int min, final int max, final ChatFormatting... formats) {
        final var prefix = mode == Mode.IN_COMMENTS ? C_PREFIX : "\\s*";
        final var level = min == max ? String.valueOf(min) : min + "," + max;
        final var pattern =
            String.format("^%s(?<symbols>#{%s}\\s+)(?<text>.+)$", prefix, level);
        return RegexHighlighter.builder(Pattern.compile(pattern, Pattern.MULTILINE))
            .linter("symbols", sigil(mode))
            .linter("text", Linter.from(formats))
            .build()
            .canBeSplit();
    }

    private static Highlighter multilineLayer(final Mode mode, final Pattern pattern, final ChatFormatting... formats) {
        return RegexHighlighter.builder(pattern)
            .linter("left", sigil(mode))
            .linter("right", sigil(mode))
            .linter("text", multiline(mode, Linter.from(formats)))
            .build()
            .canBeSplit();
    }

    private static Highlighter link(final Mode mode) {
        return RegexHighlighter.builder(LINK)
            .linter("lb", sigil(mode))
            .linterFunction("text", MarkdownLinter::linkText)
            .linter("rb", sigil(mode))
            .linter("lp", sigil(mode))
            .linter("link", sigil(mode))
            .linter("rp", sigil(mode))
            .build()
            .canBeSplit();
    }

    private static Linter linkText(final Matcher matcher) {
        final var link = matcher.group("link");
        return text -> Component.literal(text).withStyle(
            Style.EMPTY.withColor(ChatFormatting.BLUE)
                .withUnderlined(true)
                .withClickEvent(clickToGo(link))
                .withHoverEvent(displayOnHover(link)));
    }

    private static Highlighter unorderedList(final Mode mode) {
        var pattern = "^(?<bullet>[ \\t]*?[-+*])\\s+(?<entry>.+)$";
        if (mode == Mode.IN_COMMENTS) {
            pattern = pattern.replace("^", "^" + C_PREFIX);
        }
        return RegexHighlighter.builder(Pattern.compile(pattern, Pattern.MULTILINE))
            .linter("bullet", bullet(mode))
            .linter("entry", recurse(mode))
            .build();
    }

    private static Highlighter codeBlock(final Mode mode) {
        var pattern = "^(?<left>```)(?:\\s*(?<lang>\\w+)\\s*(?<nl1>\n))?(?<body>[\\s\\S]*?)^(?<right>```)$(?<nl2>\\s*?\n)?";
        if (mode == Mode.IN_COMMENTS) {
            pattern = pattern.replace("^", "^" + C_PREFIX);
        }
        return RegexHighlighter.builder(Pattern.compile(pattern, Pattern.MULTILINE))
            .linter("left", sigil(mode))
            .linter("lang", sigil(mode))
            .linter("nl1", sigil(mode))
            .linterFunction("body", matcher -> lintCode(mode, matcher))
            .linter("right", sigil(mode))
            .linter("nl2", sigil(mode))
            .build();
    }

    private static Linter lintCode(final Mode mode, final Matcher matcher) {
        if (mode == Mode.IN_COMMENTS) {
            return forEachLine(Linter.from(ChatFormatting.GRAY));
        }
        final var lang = Objects.requireNonNullElse(matcher.group("lang"), "");
        return Linters.get(lang, Linter.from(ChatFormatting.GRAY))
            .compose(t -> "  " + t.replace("\n", "\n  ")); // indent since we can't do background colors
    }

    private static Linter sigil(final Mode mode) {
        return mode == Mode.DISPLAY ? Linter.delete() : Linter.from(ChatFormatting.DARK_GRAY);
    }

    private static Linter bullet(final Mode mode) {
        return mode == Mode.DISPLAY
            ? t -> Component.literal(t.replaceAll("[-+*]", "  •")).withStyle(ChatFormatting.BOLD)
            : Linter.from(ChatFormatting.DARK_GRAY);
    }

    private static Linter multiline(final Mode mode, final Linter linter) {
        return mode == Mode.IN_COMMENTS ? forEachLine(linter) : linter;
    }

    private static Linter forEachLine(final Linter linter) {
        return Linter.of(RegexHighlighter.builder(FOR_EACH_LINE).linter("line", linter).build());
    }

    private static Linter recurse(final Mode mode) {
        return t -> {
            final var linter = mode == Mode.DISPLAY ? DISPLAY : mode == Mode.IN_COMMENTS ? IN_COMMENTS : DEFAULT;
            return Objects.requireNonNull(linter, "out of order").lint(t);
        };
    }

    private static String wrapParagraphs(final String text) {
        final var lines = text.split("\r?\n");
        final var sb = new StringBuilder();
        final var paragraph = new StringBuilder();
        boolean preformatted = false;

        for (final String line : lines) {
            if (line.matches("^\\s*`{3}.*$")) {
                preformatted = !preformatted;
            }
            if (preformatted) {
                sb.append(line).append('\n');
                continue;
            }

            if (LIKELY_PARAGRAPH.matcher(line).matches()) {
                if (!paragraph.isEmpty()) paragraph.append(' ');
                paragraph.append(line.trim());
            } else {
                if (!paragraph.isEmpty()) {
                    sb.append(paragraph).append("\n");
                    paragraph.setLength(0);
                }
                sb.append(line).append('\n');
            }
            if (line.isBlank() && !paragraph.isEmpty()) {
                sb.append(paragraph).append("\n");
                paragraph.setLength(0);
            }
        }
        if (!paragraph.isEmpty()) {
            sb.append(paragraph).append("\n");
        }
        return sb.toString();
    }

    public enum Mode { DISPLAY, IN_COMMENTS, DEFAULT }
}
