package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static personthecat.catlib.command.CommandUtils.displayOnHover;

public final class StackTraceLinter {
    private static final Pattern AT_PATTERN = Pattern.compile("\\bat\\s", Pattern.MULTILINE);
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?<=[\\s/])[a-z]+(\\.[a-z]+)+(?=\\.)", Pattern.MULTILINE);
    private static final Pattern METHOD_PATTERN = Pattern.compile("[\\w$]+(?=\\()", Pattern.MULTILINE);
    private static final Pattern SPECIAL_METHOD_PATTERN = Pattern.compile("(<init>|<clinit>)(?=\\()", Pattern.MULTILINE);
    private static final Pattern LINE_PATTERN = Pattern.compile("(?<=\\()(\\w+\\.\\w+:\\d+)(?=\\))", Pattern.MULTILINE);
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(":\\s(.+)", Pattern.MULTILINE);

    public static final Linter INSTANCE = Linter.of(
        new RegexHighlighter(AT_PATTERN, ChatFormatting.DARK_GREEN),
        new SourceHighlighter(),
        new RegexHighlighter(PACKAGE_PATTERN, ChatFormatting.GRAY),
        new RegexHighlighter(METHOD_PATTERN, ChatFormatting.GOLD, ChatFormatting.ITALIC),
        new RegexHighlighter(SPECIAL_METHOD_PATTERN, ChatFormatting.GOLD, ChatFormatting.BOLD, ChatFormatting.ITALIC),
        new RegexHighlighter(LINE_PATTERN, ChatFormatting.DARK_PURPLE, ChatFormatting.UNDERLINE),
        new RegexHighlighter(MESSAGE_PATTERN, ChatFormatting.RED)
    ).compose(StackTraceLinter::collapsePackages);

    private StackTraceLinter() {}

    private static String collapsePackages(final String stacktrace) {
        final StringBuilder sb = new StringBuilder(stacktrace.length());
        final Matcher matcher = PACKAGE_PATTERN.matcher(stacktrace);

        int end = 0;
        while (matcher.find()) {
            sb.append(stacktrace, end, matcher.start());

            final String match = matcher.group(0);
            final String[] packs = match.split("\\.");
            sb.append(packs[0].charAt(0));

            for (int i = 1; i < packs.length; i++) {
                sb.append('.').append(packs[i].charAt(0));
            }
            end = matcher.end();
        }
        if (end > 0) {
            sb.append(stacktrace, end, stacktrace.length());
        }

        return sb.toString();
    }

    public static class SourceHighlighter implements Highlighter {
        private static final Pattern SOURCE_PATTERN = Pattern.compile("(?<=\\s)(\\S*)/", Pattern.MULTILINE);

        @Override
        public Instance get(final String text) {
            return new SourceHighlighterInstance(text);
        }

        public static class SourceHighlighterInstance implements Instance {
            final Matcher matcher;
            final String text;
            boolean found;

            public SourceHighlighterInstance(final String text) {
                this.matcher = SOURCE_PATTERN.matcher(text);
                this.text = text;
                this.found = matcher.find();
            }

            @Override
            public void next() {
                this.found = this.matcher.find();
            }

            @Override
            public boolean found() {
                return this.found;
            }

            @Override
            public int start() {
                return this.matcher.start(1);
            }

            @Override
            public int end() {
                return this.matcher.end(1);
            }

            @Override
            public Component replacement() {
                return Component.literal("...").withStyle(
                    Style.EMPTY.withColor(ChatFormatting.GRAY)
                        .withUnderlined(true)
                        .withHoverEvent(displayOnHover(this.matcher.group())));
            }
        }
    }
}
