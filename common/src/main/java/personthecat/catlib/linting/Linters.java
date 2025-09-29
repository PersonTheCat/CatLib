package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import xjs.data.comments.CommentStyle;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.token.TokenType;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class Linters {
    private static final Map<String, Linter> REGISTRY = new ConcurrentHashMap<>();
    public static final Linter DJS = jsonLike("djs");
    public static final Linter JSONC = jsonLike("jsonc");
    public static final Linter HJSON = jsonLike("hjson");
    public static final Linter MD = MarkdownLinter.DISPLAY;
    public static final Linter JSON = JSONC;
    public static final Linter NONE = Component::literal;

    static {
        register("djs", DJS);
        register("jsonc", JSONC);
        register("hjson", HJSON);
        register("json", JSON);
        register("md", MD);
    }

    private Linters() {}

    public static void register(final String ext, final Linter linter) {
        REGISTRY.put(JsonContext.getFormat(ext), linter);
    }

    public static Linter get(final Path path) {
        return get(JsonContext.getFormat(path));
    }

    public static Linter get(final Path path, @Nullable Linter def) {
        return get(JsonContext.getFormat(path), def);
    }

    public static Linter get(final String ext) {
        return Objects.requireNonNull(get(ext, null), () -> "No linter for " + ext);
    }

    public static Linter get(final String ext, @Nullable Linter def) {
        return REGISTRY.getOrDefault(JsonContext.getFormat(ext), def);
    }

    private static Linter jsonLike(String format) {
        return Linter.of(
            TokenHighlighter.builder()
                .tokenizer(JsonContext.getTokenizer(format))
                .key(ChatFormatting.LIGHT_PURPLE)
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
}
