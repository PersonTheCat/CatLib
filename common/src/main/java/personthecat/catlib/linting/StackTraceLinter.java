package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StackTraceLinter extends SyntaxLinter {

    private static final Pattern AT_PATTERN = Pattern.compile("\\bat\\s", Pattern.MULTILINE);
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?<=[\\s/])[a-z]+(\\.[a-z]+)+(?=\\.)", Pattern.MULTILINE);
    private static final Pattern METHOD_PATTERN = Pattern.compile("[\\w$]+(?=\\()", Pattern.MULTILINE);
    private static final Pattern SPECIAL_METHOD_PATTERN = Pattern.compile("(<init>|<clinit>)(?=\\()", Pattern.MULTILINE);
    private static final Pattern LINE_PATTERN = Pattern.compile("(?<=\\()(\\w+\\.\\w+:\\d+)(?=\\))", Pattern.MULTILINE);
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(":\\s(.+)", Pattern.MULTILINE);

    private static final Highlighter[] HIGHLIGHTERS = {
        new RegexHighlighter(AT_PATTERN, color(ChatFormatting.DARK_GREEN)),
        new SourceHighlighter(),
        new RegexHighlighter(PACKAGE_PATTERN, color(ChatFormatting.GRAY)),
        new RegexHighlighter(METHOD_PATTERN, color(ChatFormatting.GOLD).withItalic(true)),
        new RegexHighlighter(SPECIAL_METHOD_PATTERN, color(ChatFormatting.GOLD).withItalic(true).withBold(true)),
        new RegexHighlighter(LINE_PATTERN, color(ChatFormatting.DARK_PURPLE).applyFormat(ChatFormatting.UNDERLINE)),
        new RegexHighlighter(MESSAGE_PATTERN, color(ChatFormatting.RED))
    };

    public static final StackTraceLinter INSTANCE = new StackTraceLinter();

    private StackTraceLinter() {
        super(HIGHLIGHTERS);
    }

    public static Component format(final String stacktrace) {
        return INSTANCE.lint(collapsePackages(stacktrace));
    }

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
                return stc("...").withStyle(color(ChatFormatting.GRAY).withUnderlined(true).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, stc(this.matcher.group()))));
            }
        }
    }
}
