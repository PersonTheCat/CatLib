package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A highlighter which applies a single pattern based on a regular expression.
 *
 * <p>Note that, by default, any part of the expression matched will be consumed
 * by this highlighter. To explicitly match the individual groups within a pattern,
 * apply the <code>useGroups</code> flag to the constructor.
 */
public class RegexHighlighter implements Highlighter {
    private final Pattern pattern;
    private final Linter linter;
    private final boolean useGroups;

    public RegexHighlighter(final Pattern pattern, final ChatFormatting... formats) {
        this(pattern, Linter.from(formats));
    }

    public RegexHighlighter(final Pattern pattern, final Style style) {
        this(pattern, Linter.from(style));
    }

    public RegexHighlighter(final Pattern pattern, final Linter linter) {
        this(pattern, linter, false);
    }

    public RegexHighlighter(final Pattern pattern, final Linter linter, final boolean useGroups) {
        this.pattern = pattern;
        this.linter = Objects.requireNonNull(linter);
        this.useGroups = useGroups;
    }

    @Override
    public Instance get(final String text) {
        return new RegexHighlighterInstance(text);
    }

    public class RegexHighlighterInstance implements Instance {
        final Matcher matcher;
        final String text;
        final int groups;
        boolean found;
        int group;

        public RegexHighlighterInstance(final String text) {
            this.matcher = pattern.matcher(text);
            this.text = text;
            this.groups = useGroups ? matcher.groupCount() : 0;
            this.found = matcher.find();
            this.group = found && groups > 0 ? 1 : 0;
        }

        @Override
        public void next() {
            if (this.group < this.groups) {
                this.group++;
            } else {
                this.found = this.matcher.find();
            }
        }

        @Override
        public boolean found() {
            return this.found;
        }

        @Override
        public int start() {
            return this.matcher.start(this.group);
        }

        @Override
        public int end() {
            return this.matcher.end(this.group);
        }

        @Override
        public Component replacement() {
            return RegexHighlighter.this.linter.lint(this.text.substring(this.start(), this.end()));
        }
    }
}
