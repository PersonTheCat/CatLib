package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import xjs.data.comments.CommentStyle;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.token.TokenType;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static personthecat.catlib.command.CommandUtils.clickToGo;

public final class Linters {
    private static final Map<String, LinterMap> REGISTRY = new ConcurrentHashMap<>();
    public static final Linter DJS = jsonLike("djs");
    public static final Linter JSONC = jsonLike("jsonc");
    public static final Linter HJSON = jsonLike("hjson");
    public static final Linter JSON = JSONC;
    public static final Linter MD = MarkdownLinter.DISPLAY;
    public static final Linter DJS_DETAILS = jsonLikeDetails("djs");
    public static final Linter JSONC_DETAILS = jsonLikeDetails("jsonc");
    public static final Linter HJSON_DETAILS = jsonLikeDetails("hjson");
    public static final Linter JSON_DETAILS = JSONC_DETAILS;
    public static final Linter NONE = Component::literal;

    static {
        register(LinterType.HIGHLIGHTS, "djs", DJS);
        register(LinterType.HIGHLIGHTS, "jsonc", JSONC);
        register(LinterType.HIGHLIGHTS, "hjson", HJSON);
        register(LinterType.HIGHLIGHTS, "json", JSON);
        register(LinterType.ATOMIC_RENDER, "md", MD);
        register(LinterType.DETAILS, "djs", DJS_DETAILS);
        register(LinterType.DETAILS, "jsonc", JSONC_DETAILS);
        register(LinterType.DETAILS, "hjson", HJSON_DETAILS);
        register(LinterType.DETAILS, "json", JSON_DETAILS);
    }

    private Linters() {}

    public static void register(final LinterType type, final String ext, final Linter linter) {
        REGISTRY.computeIfAbsent(JsonContext.getFormat(ext), e -> new LinterMap()).put(type, linter);
    }

    public static Linter get(final LinterType type, final Path path) {
        return get(type, JsonContext.getFormat(path));
    }

    public static Linter get(final LinterType type, Path path, @Nullable Linter def) {
        return get(type, JsonContext.getFormat(path), def);
    }

    public static Linter get(final LinterType type, final String ext) {
        return Objects.requireNonNull(get(type, ext, null), () -> "No linter for " + ext);
    }

    public static Linter get(final LinterType type, String ext, @Nullable Linter def) {
        final var set = REGISTRY.get(JsonContext.getFormat(ext));
        if (set == null) return def;
        return Objects.requireNonNullElse(set.get(type), def);
    }

    private static Linter jsonLike(String format) {
        return Linter.of(
            TokenHighlighter.builder()
                .tokenizer(JsonContext.getTokenizer(format))
                .key(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withClickEvent(clickToGo("https://www.google.com")))
                .token(TokenType.COMMENT, ChatFormatting.GRAY)
                .comment(CommentStyle.MULTILINE_DOC, MarkdownLinter.IN_COMMENTS.scissor(3, 2).withBackground(ChatFormatting.DARK_GREEN))
                .comment(CommentStyle.LINE_DOC, MarkdownLinter.IN_COMMENTS.withBackground(ChatFormatting.DARK_GREEN))
                .comment(Pattern.compile("^\\s*(todo|to-do)", Pattern.CASE_INSENSITIVE), ChatFormatting.YELLOW)
                .word(Pattern.compile("^(?:true|false)$"), ChatFormatting.GOLD)
                .word(Pattern.compile("^null$"), ChatFormatting.RED)
                .token(TokenType.NUMBER, ChatFormatting.AQUA)
                .token(TokenType.STRING, ChatFormatting.DARK_GREEN)
                .token(TokenType.WORD, ChatFormatting.DARK_GREEN)
                .token(TokenType.SYMBOL, ChatFormatting.WHITE)
                .build()
        );
    }

    private static Linter jsonLikeDetails(String format) {
        return Linter.of(new SyntaxErrorHighlighter(JsonContext.getParser(format)));
    }

    private static class LinterMap extends ConcurrentHashMap<LinterType, Linter> {
        @Override
        public Linter get(Object key) {
            return Objects.requireNonNullElseGet(super.get(key), () -> super.get(LinterType.HIGHLIGHTS));
        }
    }
}
