package personthecat.catlib.linting;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.intellij.lang.annotations.RegExp;
import personthecat.catlib.command.annotations.Nullable;

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
    private final @Nullable Style style;
    private final boolean useGroups;

    public RegexHighlighter(final @RegExp String pattern, final @Nullable Style style) {
        this(Pattern.compile(pattern, Pattern.MULTILINE), style);
    }

    public RegexHighlighter(final Pattern pattern, final @Nullable Style style) {
        this(pattern, style, false);
    }

    public RegexHighlighter(final Pattern pattern, final @Nullable Style style, final boolean useGroups) {
        this.pattern = pattern;
        this.style = style;
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
        int m;

        public RegexHighlighterInstance(final String text) {
            this.matcher = pattern.matcher(text);
            this.text = text;
            this.groups = useGroups ? matcher.groupCount() : 0;
            this.found = matcher.find();
            this.group = found && groups > 0 ? 1 : 0;
            this.m = 0;
        }

        @Override
        public void next() {
            if (this.group < this.groups) {
                this.group++;
            } else {
                this.found = this.matcher.find();
                if (this.found) {
                    this.m++;
                }
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
            return Component.literal(this.text.substring(this.start(), this.end())).setStyle(this.getStyle());
        }

        private Style getStyle() {
            return SyntaxLinter.checkStyle(RegexHighlighter.this.style, this.m);
        }
    }
}
