package personthecat.catlib.linting;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import oshi.annotation.concurrent.ThreadSafe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * The primary implementation supporting syntax highlighters for CatLib.
 *
 * <p>This object is valid and safe to use in a multithreaded context.
 */
@ThreadSafe
final class LinterDelegate implements Linter {

    /** Whichever targets the author has selected for highlighting their text. */
    private final List<Highlighter> highlighters;

    /**
     * Constructs a new SyntaxLinter used for linting in the standard MC chat.
     *
     * @param highlighters An array of text highlighters.
     */
    LinterDelegate(final List<Highlighter> highlighters) {
        this.highlighters = highlighters;
    }

    /**
     * Generates a formatted text component containing the input text the requested
     * highlights.
     *
     * @param text Any text of the expected syntax.
     * @return A linted text output containing the original message.
     */
    @Override
    public Component lint(final String text) {
        final var formatted = Component.empty();
        final Context ctx = new Context(text, this.highlighters);

        Highlighter.Instance h;
        int i = 0;
        while ((h = ctx.next(i)) != null) {
            final int start = h.start();
            final int end = h.end();

            if (start - i > 0) {
                // Append unformatted text;
                formatted.append(Component.literal(text.substring(i, start)));
            }
            formatted.append(h.replacement());
            ctx.skipTo(end);
            i = end;
        }

        return formatted.append(Component.literal(text.substring(i)));
    }

    /**
     * The context used for highlighting text output. Essentially just a list of
     * {@link Matcher} -> {@link Style} for the given text.
     */
    private static class Context {
        final List<Highlighter.Instance> highlighters = new ArrayList<>();
        final String text;

        Context(final String text, final List<Highlighter> highlighters) {
            this.text = text;
            for (final Highlighter highlighter : highlighters) {
                this.highlighters.add(highlighter.get(text));
            }
        }

        @Nullable
        Highlighter.Instance next(final int i) {
            // Figure out whether any other matches have been found;
            int start = Integer.MAX_VALUE;
            Highlighter.Instance first = null;
            for (final Highlighter.Instance h : this.highlighters) {
                if (!h.found()) continue;
                final int mStart = h.start();

                if (mStart >= i && mStart < start) {
                    start = mStart;
                    first = h;
                }
            }
            return first;
        }

        void skipTo(final int i) {
            for (final Highlighter.Instance h : this.highlighters) {
                if (!h.found()) continue;
                if (h.end() <= i) {
                    h.next();
                }
            }
        }
    }
}
