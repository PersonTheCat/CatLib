package personthecat.catlib.linting;

import com.google.common.collect.ImmutableMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.Nullable;
import xjs.comments.CommentStyle;
import xjs.jel.exception.JelException;
import xjs.jel.sequence.JelType;
import xjs.jel.sequence.Sequence;
import xjs.jel.serialization.sequence.Sequencer;
import xjs.serialization.Span;
import xjs.serialization.token.CommentToken;
import xjs.serialization.token.ContainerToken;
import xjs.serialization.token.ParsedToken;
import xjs.serialization.token.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static personthecat.catlib.linting.SyntaxLinter.color;
import static personthecat.catlib.linting.SyntaxLinter.error;
import static personthecat.catlib.linting.SyntaxLinter.stc;

public class JelHighlighter implements SyntaxLinter.Highlighter {

    public static final Object TODO_MARKER = new Object();
    public static final Pattern TODO_PATTERN =
        Pattern.compile("^\\s*todo\\s*:.*", Pattern.CASE_INSENSITIVE);

    public static final Map<Object, Style> DEFAULT_STYLE_MAP =
        ImmutableMap.<Object, Style>builder()
            .put(JelType.KEY, color(ChatFormatting.LIGHT_PURPLE))
            .put(JelType.CALL, color(ChatFormatting.YELLOW).withItalic(true))
            .put(JelType.REFERENCE, color(ChatFormatting.LIGHT_PURPLE))
            .put(JelType.REFERENCE_EXPANSION, color(ChatFormatting.LIGHT_PURPLE))
            .put(JelType.FLAG, color(ChatFormatting.GOLD))
            .put(JelType.IMPORT, color(ChatFormatting.GOLD))
            .put(JelType.MATCH, color(ChatFormatting.GOLD))
            .put(JelType.DELEGATE, color(ChatFormatting.YELLOW))
            .put(JelType.BOOLEAN, color(ChatFormatting.GOLD))
            .put(JelType.INDEX, color(ChatFormatting.AQUA))
            .put(JelType.NUMBER, color(ChatFormatting.AQUA))
            .put(TokenType.NUMBER, color(ChatFormatting.AQUA))
            .put(JelType.NULL, color(ChatFormatting.RED))
            .put(TokenType.STRING, color(ChatFormatting.GREEN))
            .put(JelType.STRING, color(ChatFormatting.GREEN))
            .put(CommentStyle.BLOCK, color(ChatFormatting.GRAY).withItalic(true))
            .put(CommentStyle.LINE, color(ChatFormatting.GRAY).withItalic(true))
            .put(CommentStyle.HASH, color(ChatFormatting.GRAY).withItalic(true))
            .put(CommentStyle.LINE_DOC, color(ChatFormatting.GREEN).withItalic(true))
            .put(CommentStyle.MULTILINE_DOC, color(ChatFormatting.GREEN).withItalic(true))
            .put(TODO_MARKER, color(ChatFormatting.YELLOW).withItalic(true))
            .build();

    protected final Map<Object, Style> styleMap;

    public JelHighlighter() {
        this(DEFAULT_STYLE_MAP);
    }

    public JelHighlighter(final Map<Object, Style> styleMap) {
        this.styleMap = styleMap;
    }

    @Override
    public Instance get(final String text) {
        try {
            final Sequence<?> sequence = Sequencer.JEL.parse(text);
            final List<Span<?>> spans = new ArrayList<>();
            trueFlatten(spans, sequence);
            return new JelHighlighterInstance(this.styleMap, spans, text);
        } catch (final JelException e) {
            return new ExceptionHighlighterInstance(e, text);
        }
    }

    protected static void trueFlatten(final List<Span<?>> spans, final Sequence<?> sequence) {
        for (final Span<?> flat : sequence.flatten()) {
            if (flat instanceof ContainerToken container) {
                trueFlatten(spans, container);
            } else if (flat.type() != TokenType.BREAK && flat.length() > 0) {
                spans.add(flat);
            }
        }
    }

    protected static void trueFlatten(final List<Span<?>> spans, final ContainerToken container) {
        for (final Span<?> s : container.viewTokens()) {
            if (s instanceof ContainerToken inner) {
                trueFlatten(spans, inner);
            } else if (s.type() != TokenType.BREAK && s.length() > 0) {
                spans.add(s);
            }
        }
    }

    protected static Object trueType(final Span<?> span) {
        if (span instanceof CommentToken c) {
            if (TODO_PATTERN.matcher(c.parsed()).matches()) {
                return TODO_MARKER;
            }
            return c.commentStyle();
        } else if (span.type() == TokenType.WORD && span instanceof ParsedToken p) {
            return switch (p.parsed()) {
                case "true", "false" -> JelType.BOOLEAN;
                case "null" -> JelType.NULL;
                default -> TokenType.WORD;
            };
        }
        return span.type();
    }

    public static class ExceptionHighlighterInstance implements Instance {
        protected final JelException ex;
        protected final List<Span<?>> spans;
        protected final String text;
        protected final String msg;
        protected @Nullable Span<?> current;
        protected int index;

        protected ExceptionHighlighterInstance(final JelException ex, final String text) {
            this.ex = ex;
            this.spans = ex.getSpans().computeIfAbsent(null, k -> Collections.emptyList());
            this.text = text;
            this.msg = ex.getMessage() + "\n" + ex.getDetails();
            this.next();
        }

        @Override
        public void next() {
            if (this.index < this.spans.size()) {
                this.current = this.spans.get(this.index++);
            } else {
                this.current = null;
            }
        }

        @Override
        public boolean found() {
            return this.current != null;
        }

        @Override
        public int start() {
            return Objects.requireNonNull(this.current).start();
        }

        @Override
        public int end() {
            return Objects.requireNonNull(this.current).end();
        }

        @Override
        public Component replacement() {
            final Span<?> s = Objects.requireNonNull(this.current);
            return stc(s.textOf(this.text)).withStyle(error(this.msg));
        }
    }

    public static class JelHighlighterInstance implements Instance {
        protected final Map<Object, Style> styleMap;
        protected final List<Span<?>> spans;
        protected final String text;
        protected @Nullable Span<?> current;
        protected int index;

        protected JelHighlighterInstance(
                final Map<Object, Style> styleMap,
                final List<Span<?>> spans,
                final String text) {
            this.styleMap = styleMap;
            this.spans = spans;
            this.text = text;
            this.next();
        }

        @Override
        public void next() {
            if (this.index < this.spans.size()) {
                this.current = this.spans.get(this.index++);
            } else {
                this.current = null;
            }
        }

        @Override
        public boolean found() {
            return this.current != null;
        }

        @Override
        public int start() {
            return Objects.requireNonNull(this.current).start();
        }

        @Override
        public int end() {
            return Objects.requireNonNull(this.current).end();
        }

        @Override
        public Component replacement() {
            final Span<?> s = Objects.requireNonNull(this.current);
            final Style style = this.styleMap.get(trueType(s));
            final TextComponent replacement = stc(s.textOf(this.text));
            return style != null ? replacement.withStyle(style) : replacement;
        }
    }
}
