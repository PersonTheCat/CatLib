package personthecat.catlib.linting;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.BitSet;

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
public class UnbalancedTokenHighlighter implements Highlighter {
    public static final UnbalancedTokenHighlighter INSTANCE = new UnbalancedTokenHighlighter();
    private static final Style UNCLOSED_ERROR = Linter.error("catlib.errorText.unclosed");
    private static final Style UNEXPECTED_ERROR = Linter.error("catlib.errorText.unclosed");

    private UnbalancedTokenHighlighter() {}

    @Override
    public Highlighter.Instance get(final String text) {
        return new Instance(text);
    }

    private static class Instance implements Highlighter.Instance {
        final BitSet unclosed = new BitSet();
        final BitSet unexpected = new BitSet();
        int unclosedIndex = 0;
        int unexpectedIndex = 0;
        final String text;

        private Instance(final String text) {
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
            return Component.literal(String.valueOf(c)).withStyle(style);
        }
    }
}
