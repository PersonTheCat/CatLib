package personthecat.catlib.linting;

import com.google.common.base.Preconditions;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A highlighter which applies a single pattern based on a regular expression.
 *
 * <p>Note that, by default, any part of the expression matched will be consumed
 * by this highlighter. To explicitly match the individual groups within a pattern,
 * apply the <code>useGroups</code> flag to the constructor.
 */
public record RegexHighlighter(Pattern pattern, MatchFunction[] linters, boolean useGroups) implements Highlighter {

    public RegexHighlighter {
        Preconditions.checkArgument(linters.length > 0, "Linters must not be empty");
    }

    public static Builder builder(final Pattern pattern) {
        return new Builder(pattern);
    }

    public static RegexHighlighter fromMap(
            final Pattern pattern, final Map<String, MatchFunction> linters, final boolean useGroups) {
        return new RegexHighlighter(pattern, buildLinters(pattern.namedGroups(), linters), useGroups);
    }

    // User gives us groups by name, but we match by index
    private static MatchFunction[] buildLinters(final Map<String, Integer> groups, final Map<String, MatchFunction> source) {
        final var linters = new MatchFunction[source.size() + 1];
        source.forEach((name, linter) -> linters[name.isEmpty() ? 0 : groups.get(name)] = linter);
        return linters;
    }

    @Override
    public Highlighter.Instance get(final String text) {
        return new Instance(text);
    }

    private Linter getLinter(final Matcher matcher, int group) {
        group = Math.clamp(group, 0, this.linters.length - 1);
        return Objects.requireNonNullElseGet(this.linters[group], () -> this.linters[0]).apply(matcher);
    }

    public static class Builder {
        private final Map<String, MatchFunction> linters = new HashMap<>();
        private final Pattern pattern;
        private @Nullable Boolean useGroups;

        public Builder(final Pattern pattern) {
            this.pattern = pattern;
        }

        public Builder linter(final ChatFormatting... formats) {
            return this.linter(Linter.from(formats));
        }

        public Builder linter(final Linter linter) {
            return this.linterFunction(matcher -> linter);
        }

        public Builder linterFunction(final MatchFunction linter) {
            return this.linterFunction("", linter);
        }

        public Builder linter(final String group, final ChatFormatting... formats) {
            return this.linter(group, Linter.from(formats));
        }

        public Builder linter(final String group, final Linter linter) {
            return this.linterFunction(group, matcher -> linter);
        }

        public Builder linterFunction(final String group, final MatchFunction linter) {
            this.linters.put(group, linter);
            return this;
        }

        public Builder useGroups(final boolean useGroups) {
            this.useGroups = useGroups;
            return this;
        }

        public RegexHighlighter build() {
            final var useGroups = Objects.requireNonNullElseGet(this.useGroups, this::shouldUseGroups);
            return RegexHighlighter.fromMap(pattern, this.linters, useGroups);
        }

        private boolean shouldUseGroups() {
            return this.linters.size() > 1 || !this.linters.containsKey("");
        }
    }

    private class Instance implements Highlighter.Instance {
        final Matcher matcher;
        final int groups;
        @Nullable Linter linter;
        int group;

        private Instance(final String text) {
            this.matcher = RegexHighlighter.this.pattern.matcher(text);
            this.groups = RegexHighlighter.this.useGroups ? this.matcher.groupCount() : 0;
            this.group = -1;
            this.next();
        }

        @Override
        public void next() {
            do {
                if (this.group >= 0 && this.group < this.groups) {
                    this.group++;
                } else if (this.matcher.find()) {
                    this.group = this.groups > 0 ? 1 : 0;
                } else {
                    this.linter = null;
                    return;
                }
            } while (this.start() < 0
                || (this.linter = RegexHighlighter.this.getLinter(this.matcher, this.group)) == null);
        }

        @Override
        public boolean found() {
            return this.linter != null;
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
        public Linter match() {
            return Objects.requireNonNull(this.linter, "no such element");
        }
    }

    @FunctionalInterface
    public interface MatchFunction extends Function<Matcher, @Nullable Linter> {}
}
