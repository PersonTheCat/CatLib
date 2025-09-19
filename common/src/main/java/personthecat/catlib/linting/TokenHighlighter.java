package personthecat.catlib.linting;

import com.google.common.collect.ImmutableMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.command.annotations.Nullable;
import xjs.data.StringType;
import xjs.data.comments.CommentStyle;
import xjs.data.serialization.token.DjsTokenizer;
import xjs.data.serialization.token.SymbolToken;
import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.token.TokenType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public class TokenHighlighter implements Highlighter {
    private final Function<String, TokenStream> tokenizer;
    private final Map<TokenType, Style> tokenStyles;
    private final Map<StringType, Style> stringStyles;
    private final Map<Pattern, Style> stringPatternStyles;
    private final Map<CommentStyle, Style> commentStyles;
    private final Map<Pattern, Style> commentPatternStyles;
    private final Map<Pattern, Style> wordStyles;
    private final @Nullable Style keyStyle;

    private TokenHighlighter(
            Function<String, TokenStream> tokenizer,
            Map<TokenType, Style> tokenStyles,
            Map<StringType, Style> stringStyles,
            Map<Pattern, Style> stringPatternStyles,
            Map<CommentStyle, Style> commentStyles,
            Map<Pattern, Style> commentPatternStyles,
            Map<Pattern, Style> wordStyles,
            Style keyStyle) {
        this.tokenizer = tokenizer;
        this.tokenStyles = tokenStyles;
        this.stringStyles = stringStyles;
        this.stringPatternStyles = stringPatternStyles;
        this.commentStyles = commentStyles;
        this.commentPatternStyles = commentPatternStyles;
        this.wordStyles = wordStyles;
        this.keyStyle = keyStyle;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Instance get(String text) {
        return new Instance(this.tokenizer.apply(text), text);
    }

    private @Nullable Style getStyle(Token token, boolean expectingKey) {
        final var type = token.type();
        final var def = this.tokenStyles.get(type);

        if (type == TokenType.COMMENT) {
            final var s = token.parsed();
            for (final var e : this.commentPatternStyles.entrySet()) {
                if (e.getKey().matcher(s).find()) {
                    return e.getValue();
                }
            }
            return this.commentStyles.getOrDefault(token.commentStyle(), def);
        }
        if (type == TokenType.STRING || type == TokenType.WORD) {
            if (expectingKey && this.keyStyle != null) {
                return this.keyStyle;
            }
            final var s = token.parsed();
            if (type == TokenType.WORD) {
                for (final var e : this.wordStyles.entrySet()) {
                    if (e.getKey().matcher(s).find()) {
                        return e.getValue();
                    }
                }
            }
            for (final var e : this.stringPatternStyles.entrySet()) {
                if (e.getKey().matcher(s).find()) {
                    return e.getValue();
                }
            }
            final var st = type == TokenType.WORD ? StringType.IMPLICIT : token.stringType();
            return this.stringStyles.getOrDefault(st, def);
        }
        return def;
    }

    public static class Builder {
        private @NotNull Function<String, TokenStream> tokenizer = DjsTokenizer::stream;
        private final Map<TokenType, Style> tokenStyles = new HashMap<>();
        private final Map<StringType, Style> stringStyles = new HashMap<>();
        private final Map<Pattern, Style> stringPatternStyles = new HashMap<>();
        private final Map<CommentStyle, Style> commentStyles = new HashMap<>();
        private final Map<Pattern, Style> commentPatternStyles = new HashMap<>();
        private final Map<Pattern, Style> wordStyles = new HashMap<>();
        private @Nullable Style keyStyle;

        public Builder tokenizer(@NotNull Function<String, TokenStream> tokenizer) {
            this.tokenizer = tokenizer;
            return this;
        }

        public Builder token(TokenType type, ChatFormatting... formats) {
            return this.token(type, Style.EMPTY.applyFormats(formats));
        }

        public Builder token(TokenType type, Style style) {
            this.tokenStyles.put(type, style);
            return this;
        }

        public Builder string(Pattern pattern, ChatFormatting... formats) {
            return this.string(pattern, Style.EMPTY.applyFormats(formats));
        }

        public Builder string(Pattern pattern, Style style) {
            this.stringPatternStyles.put(pattern, style);
            return this;
        }

        public Builder string(StringType type, ChatFormatting... formats) {
            return this.string(type, Style.EMPTY.applyFormats(formats));
        }

        public Builder string(StringType type, Style style) {
            this.stringStyles.put(type, style);
            return this;
        }

        public Builder comment(CommentStyle cs, ChatFormatting... formats) {
            return this.comment(cs, Style.EMPTY.applyFormats(formats));
        }

        public Builder comment(CommentStyle cs, Style style) {
            this.commentStyles.put(cs, style);
            return this;
        }

        public Builder comment(Pattern pattern, ChatFormatting... formats) {
            return this.comment(pattern, Style.EMPTY.applyFormats(formats));
        }

        public Builder comment(Pattern pattern, Style style) {
            this.commentPatternStyles.put(pattern, style);
            return this;
        }

        public Builder key(ChatFormatting... formats) {
            return this.key(Style.EMPTY.applyFormats(formats));
        }

        public Builder key(@Nullable Style style) {
            this.keyStyle = style;
            return this;
        }

        public Builder word(Pattern pattern, ChatFormatting... formats) {
            return this.word(pattern, Style.EMPTY.applyFormats(formats));
        }

        public Builder word(Pattern pattern, Style style) {
            this.wordStyles.put(pattern, style);
            return this;
        }

        public TokenHighlighter build() {
            return new TokenHighlighter(
                this.tokenizer,
                ImmutableMap.copyOf(this.tokenStyles),
                ImmutableMap.copyOf(this.stringStyles),
                ImmutableMap.copyOf(this.stringPatternStyles),
                ImmutableMap.copyOf(this.commentStyles),
                ImmutableMap.copyOf(this.commentPatternStyles),
                ImmutableMap.copyOf(this.wordStyles),
                this.keyStyle);
        }
    }

    public class Instance implements Highlighter.Instance {
        private final Deque<Character> openers = new ArrayDeque<>();
        private final TokenStream.Itr itr;
        private final String text;
        private @Nullable Style foundStyle;
        private @Nullable Token current;
        private boolean isUnbalanced;

        private Instance(TokenStream stream, String text) {
            this.itr = stream.iterator();
            this.text = text;
            this.next();
        }

        @Override
        public void next() {
            this.foundStyle = null;
            this.current = null;

            while (!this.isUnbalanced && this.foundStyle == null && this.itr.hasNext()) {
                final var next = this.itr.next();
                final boolean expectingKey = this.expectingKey();
                this.trackContainers(next);
                final var style = TokenHighlighter.this.getStyle(next, expectingKey);
                this.foundStyle = SyntaxLinter.checkStyle(style, this.itr.getIndex());
                this.current = next;
            }
        }

        private void trackContainers(Token current) {
            if (current.isSymbol('{')) {
                this.openers.addFirst('{');
            } else if (current.isSymbol('[')) {
                this.openers.addFirst('[');
            } else if (current.isSymbol('}')) {
                this.isUnbalanced = this.openers.isEmpty() || this.openers.poll() != '{';
            } else if (current.isSymbol(']')) {
                this.isUnbalanced = this.openers.isEmpty() || this.openers.poll() != '[';
            }
        }

        private boolean expectingKey() {
            final var keyTracker = this.openers.peek();
            if (keyTracker == null || keyTracker == '{') {
                return this.getNextSymbol() == ':';
            }
            return false;
        }

        private char getNextSymbol() {
            int amount = 1;
            Token peek;
            while ((peek = this.itr.peek(amount++)) != null) {
                if (peek.type() == TokenType.SYMBOL) {
                    return ((SymbolToken) peek).symbol;
                }
            }
            return 0;
        }

        @Override
        public boolean found() {
            return this.foundStyle != null;
        }

        @Override
        public int start() {
            return this.current().start();
        }

        @Override
        public int end() {
            return this.current().end();
        }

        @Override
        public Component replacement() {
            return Component.literal(this.current().textOf(this.text)).withStyle(this.currentStyle());
        }

        private Token current() {
            return Objects.requireNonNull(this.current, "No such element");
        }

        private Style currentStyle() {
            return Objects.requireNonNull(this.foundStyle, "No such element");
        }
    }
}
