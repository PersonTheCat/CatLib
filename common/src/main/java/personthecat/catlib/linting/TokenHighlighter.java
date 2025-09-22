package personthecat.catlib.linting;

import com.google.common.collect.ImmutableMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.command.annotations.Nullable;
import xjs.data.StringType;
import xjs.data.comments.CommentStyle;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.token.SymbolToken;
import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.token.TokenType;
import xjs.data.serialization.token.TokenizingFunction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class TokenHighlighter implements Highlighter {
    private final TokenizingFunction tokenizer;
    private final Map<TokenType, Linter> tokenLinters;
    private final Map<StringType, Linter> stringLinters;
    private final Map<Pattern, Linter> stringPatternLinters;
    private final Map<CommentStyle, Linter> commentLinters;
    private final Map<Pattern, Linter> commentPatternLinters;
    private final Map<Pattern, Linter> wordLinters;
    private final @Nullable Linter keyLinter;

    private TokenHighlighter(
            TokenizingFunction tokenizer,
            Map<TokenType, Linter> tokenLinters,
            Map<StringType, Linter> stringLinters,
            Map<Pattern, Linter> stringPatternLinters,
            Map<CommentStyle, Linter> commentLinters,
            Map<Pattern, Linter> commentPatternLinters,
            Map<Pattern, Linter> wordLinters,
            Linter keyLinter) {
        this.tokenizer = tokenizer;
        this.tokenLinters = tokenLinters;
        this.stringLinters = stringLinters;
        this.stringPatternLinters = stringPatternLinters;
        this.commentLinters = commentLinters;
        this.commentPatternLinters = commentPatternLinters;
        this.wordLinters = wordLinters;
        this.keyLinter = keyLinter;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Highlighter.Instance get(String text) {
        return new Instance(this.tokenizer.stream(text), text);
    }

    private @Nullable Linter getLinter(Token token, boolean expectingKey) {
        final var type = token.type();
        final var def = this.tokenLinters.get(type);

        if (type == TokenType.COMMENT) {
            final var s = token.parsed();
            for (final var e : this.commentPatternLinters.entrySet()) {
                if (e.getKey().matcher(s).find()) {
                    return e.getValue();
                }
            }
            return this.commentLinters.getOrDefault(token.commentStyle(), def);
        }
        if (type == TokenType.STRING || type == TokenType.WORD) {
            if (expectingKey && this.keyLinter != null) {
                return this.keyLinter;
            }
            final var s = token.parsed();
            if (type == TokenType.WORD) {
                for (final var e : this.wordLinters.entrySet()) {
                    if (e.getKey().matcher(s).find()) {
                        return e.getValue();
                    }
                }
            }
            for (final var e : this.stringPatternLinters.entrySet()) {
                if (e.getKey().matcher(s).find()) {
                    return e.getValue();
                }
            }
            final var st = type == TokenType.WORD ? StringType.IMPLICIT : token.stringType();
            return this.stringLinters.getOrDefault(st, def);
        }
        return def;
    }

    public static class Builder {
        private @NotNull TokenizingFunction tokenizer = JsonContext.getTokenizer("djs");
        private final Map<TokenType, Linter> tokenLinters = new HashMap<>();
        private final Map<StringType, Linter> stringLinters = new HashMap<>();
        private final Map<Pattern, Linter> stringPatternLinters = new HashMap<>();
        private final Map<CommentStyle, Linter> commentLinters = new HashMap<>();
        private final Map<Pattern, Linter> commentPatternLinters = new HashMap<>();
        private final Map<Pattern, Linter> wordLinters = new HashMap<>();
        private @Nullable Linter keyLinter;

        public Builder tokenizer(@NotNull TokenizingFunction tokenizer) {
            this.tokenizer = tokenizer;
            return this;
        }

        public Builder token(TokenType type, ChatFormatting... formats) {
            return this.token(type, Linter.from(formats));
        }

        public Builder token(TokenType type, Style style) {
            return this.token(type, Linter.from(style));
        }

        public Builder token(TokenType type, Linter linter) {
            this.tokenLinters.put(type, linter);
            return this;
        }

        public Builder string(Pattern pattern, ChatFormatting... formats) {
            return this.string(pattern, Linter.from(formats));
        }

        public Builder string(Pattern pattern, Style style) {
            return this.string(pattern, Linter.from(style));
        }

        public Builder string(Pattern pattern, Linter linter) {
            this.stringPatternLinters.put(pattern, linter);
            return this;
        }

        public Builder string(StringType type, ChatFormatting... formats) {
            return this.string(type, Linter.from(formats));
        }

        public Builder string(StringType type, Style style) {
            return this.string(type, Linter.from(style));
        }

        public Builder string(StringType type, Linter linter) {
            this.stringLinters.put(type, linter);
            return this;
        }

        public Builder comment(CommentStyle cs, ChatFormatting... formats) {
            return this.comment(cs, Linter.from(formats));
        }

        public Builder comment(CommentStyle cs, Style style) {
            return this.comment(cs, Linter.from(style));
        }

        public Builder comment(CommentStyle cs, Linter linter) {
            this.commentLinters.put(cs, linter);
            return this;
        }

        public Builder comment(Pattern pattern, ChatFormatting... formats) {
            return this.comment(pattern, Linter.from(formats));
        }

        public Builder comment(Pattern pattern, Style style) {
            return this.comment(pattern, Linter.from(style));
        }

        public Builder comment(Pattern pattern, Linter linter) {
            this.commentPatternLinters.put(pattern, linter);
            return this;
        }

        public Builder key(ChatFormatting... formats) {
            return this.key(Linter.from(formats));
        }

        public Builder key(@Nullable Style style) {
            return this.key(Linter.from(style));
        }

        public Builder key(@Nullable Linter linter) {
            this.keyLinter = linter;
            return this;
        }

        public Builder word(Pattern pattern, ChatFormatting... formats) {
            return this.word(pattern, Linter.from(formats));
        }

        public Builder word(Pattern pattern, Style style) {
            return this.word(pattern, Linter.from(style));
        }

        public Builder word(Pattern pattern, Linter linter) {
            this.wordLinters.put(pattern, linter);
            return this;
        }

        public TokenHighlighter build() {
            return new TokenHighlighter(
                this.tokenizer,
                ImmutableMap.copyOf(this.tokenLinters),
                ImmutableMap.copyOf(this.stringLinters),
                ImmutableMap.copyOf(this.stringPatternLinters),
                ImmutableMap.copyOf(this.commentLinters),
                ImmutableMap.copyOf(this.commentPatternLinters),
                ImmutableMap.copyOf(this.wordLinters),
                this.keyLinter);
        }
    }

    private class Instance implements Highlighter.Instance {
        private final Deque<Character> openers = new ArrayDeque<>();
        private final TokenStream.Itr itr;
        private final String text;
        private @Nullable Linter foundLinter;
        private @Nullable Token current;
        private boolean isUnbalanced;

        private Instance(TokenStream stream, String text) {
            this.itr = stream.iterator();
            this.text = text;
            this.next();
        }

        @Override
        public void next() {
            this.foundLinter = null;
            this.current = null;

            while (!this.isUnbalanced && this.foundLinter == null && this.itr.hasNext()) {
                final var next = this.itr.next(); // todo: catch syntax | unchecked io exception
                final boolean expectingKey = this.expectingKey();
                this.trackContainers(next);
                this.foundLinter = TokenHighlighter.this.getLinter(next, expectingKey);
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
                if (peek instanceof SymbolToken) {
                    return ((SymbolToken) peek).symbol;
                }
            }
            return 0;
        }

        @Override
        public boolean found() {
            return this.foundLinter != null;
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
            return this.currentLinter().lint(this.current().textOf(this.text));
        }

        private Token current() {
            return Objects.requireNonNull(this.current, "No such element");
        }

        private Linter currentLinter() {
            return Objects.requireNonNull(this.foundLinter, "No such element");
        }
    }
}
