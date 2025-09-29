package personthecat.catlib.linting;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

record LinterDelegate(List<Highlighter> highlighters) implements Linter {

    @Override
    public Component lint(final String text) {
        final var spans = this.computeSpans(text);
        if (spans.isEmpty()) {
            return Component.literal(text);
        }
        final var boundaries = this.computeBoundaries(text, spans);
        final var result = Component.empty();

        // Iterate the atomic intervals and compose a message
        boundary: for (int i = 0; i < boundaries.size() - 1; i++) {
            final int segStart = boundaries.get(i);
            final int segEnd = boundaries.get(i + 1);
            if (segStart >= segEnd) continue;

            final var slice = text.substring(segStart, segEnd);

            // find spans that fully cover [s, e)
            final List<Span> covering = spans.stream()
                .filter(s -> s.start <= segStart && s.end >= segEnd)
                .sorted(Comparator.comparingInt(s -> s.priority))
                .toList();

            if (covering.isEmpty()) {
                result.append(Component.literal(text.substring(segStart, segEnd)));
                continue;
            }

            // If any highlighter is atomic, choose one with the highest priority and use its replacement.
            final var atomic = covering.stream()
                .filter(s -> s.highlighter.atomic())
                .min(Comparator.comparingInt(s -> s.priority));

            if (atomic.isPresent()) {
                final var span = atomic.get();
                // if we're at the boundary, we can emit the replacement
                if (segStart == span.start) {
                    result.append(span.linter.lint(text.substring(span.start, span.end)));
                }
                continue; // otherwise, we have to skip this highlighter
            }

            // all other highlighters must support sub-range replacement -> merge
            final var chars = StyledChar.fromText(slice);

            for (final var span : covering) {
                final var comp = span.linter.lint(slice);
                if (comp.equals(CommonComponents.EMPTY)) {
                    continue boundary; // support deletions explicitly
                }
                StyledChar.applyOverlay(chars, comp);
            }
            result.append(StyledChar.toComponent(chars));
        }
        return result;
    }

    private @NotNull List<Span> computeSpans(final String text) {
        final var spans = new ArrayList<Span>();
        for (int i = 0; i < this.highlighters.size(); i++) {
            final var highlighter = this.highlighters.get(i);
            final var instance = highlighter.get(text);
            for (; instance.found(); instance.next()) {
                spans.add(new Span(highlighter, instance.match(), instance.start(), instance.end(), i));
            }
        }
        return spans;
    }

    private @NotNull List<Integer> computeBoundaries(final String text, final List<Span> spans) {
        final var boundaries = new TreeSet<Integer>();
        boundaries.add(0);
        boundaries.add(text.length());
        for (final var s : spans) {
            boundaries.add(s.start);
            boundaries.add(s.end);
        }
        return new ArrayList<>(boundaries);
    }

    record Span(Highlighter highlighter, Linter linter, int start, int end, int priority) {}
}
