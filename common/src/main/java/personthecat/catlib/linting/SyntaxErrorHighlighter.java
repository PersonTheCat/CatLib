package personthecat.catlib.linting;

import personthecat.catlib.command.annotations.Nullable;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.Span;
import xjs.data.serialization.parser.ParsingFunction;

import java.util.Objects;

public record SyntaxErrorHighlighter(ParsingFunction parser) implements Highlighter {

    @Override
    public Highlighter.Instance get(String text) {
        return new Instance(this.parser, text);
    }

    private static class Instance implements Highlighter.Instance {
        private @Nullable Span<String> error;

        private Instance(ParsingFunction parser, String text) {
            try {
                parser.parse(text);
            } catch (final SyntaxException e) {
                this.error = convertToSpan(text, e);
            }
        }

        @Override
        public void next() {
            this.error = null;
        }

        @Override
        public boolean found() {
            return this.error != null;
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
        public Linter match() {
            return Linter.from(Linter.error(this.current().type()));
        }

        private Span<String> current() {
            return Objects.requireNonNull(this.error, "no such element");
        }

        private static Span<String> convertToSpan(String text, SyntaxException e) {
            final var start = indexOf(text, e.getLine(), e.getColumn());
            final var end = text.indexOf('\n', start + 1);
            return new Span<>(start, end == -1 ? text.length() : end, e.getLine(), e.getColumn(), e.getMessage());
        }

        // scan to reconstruct index data not saved by the API
        private static int indexOf(String text, int row, int col) {
            if (row == 0) {
                return col;
            }
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n' && --row == 0) {
                    return i + col + 1;
                }
            }
            throw new IllegalStateException("Invalid row, col: " + row + "," + col);
        }
    }
}
