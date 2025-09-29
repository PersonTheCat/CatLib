package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static personthecat.catlib.command.CommandUtils.displayOnHover;

public final class StackTraceLinter {
    private static final Pattern AT_PATTERN = Pattern.compile("\\bat\\s", Pattern.MULTILINE);
    private static final Pattern SOURCE_PATTERN = Pattern.compile("(?<=\\s)(\\S*)/", Pattern.MULTILINE);
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?<=[\\s/])[a-z]+(\\.[a-z]+)+(?=\\.)", Pattern.MULTILINE);
    private static final Pattern METHOD_PATTERN = Pattern.compile("[\\w$]+(?=\\()", Pattern.MULTILINE);
    private static final Pattern SPECIAL_METHOD_PATTERN = Pattern.compile("(<init>|<clinit>)(?=\\()", Pattern.MULTILINE);
    private static final Pattern LINE_PATTERN = Pattern.compile("(?<=\\()(\\w+\\.\\w+:\\d+)(?=\\))", Pattern.MULTILINE);
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(":\\s(.+)", Pattern.MULTILINE);

    private static final Linter COLLAPSE =
        text -> Component.literal(collapsePackage(text)).withStyle(underlineInGray(text));
    private static final Linter ELIDE =
        text -> Component.literal("...").withStyle(underlineInGray(text));

    public static final Linter INSTANCE = Linter.of(
        RegexHighlighter.builder(AT_PATTERN).linter(ChatFormatting.DARK_GREEN).build(),
        RegexHighlighter.builder(SOURCE_PATTERN).linter(ELIDE).useGroups(true).build(),
        RegexHighlighter.builder(PACKAGE_PATTERN).linter(COLLAPSE).build(),
        RegexHighlighter.builder(METHOD_PATTERN).linter(ChatFormatting.GOLD, ChatFormatting.ITALIC).build(),
        RegexHighlighter.builder(SPECIAL_METHOD_PATTERN).linter(ChatFormatting.GOLD, ChatFormatting.BOLD, ChatFormatting.ITALIC).build(),
        RegexHighlighter.builder(LINE_PATTERN).linter(ChatFormatting.DARK_PURPLE, ChatFormatting.UNDERLINE).build(),
        RegexHighlighter.builder(MESSAGE_PATTERN).linter(ChatFormatting.RED).build()
    );

    private StackTraceLinter() {}

    private static String collapsePackage(final String pkg) {
        return Stream.of(pkg.split("\\.")).map(p -> String.valueOf(p.charAt(0))).collect(Collectors.joining(","));
    }

    private static Style underlineInGray(final String text) {
        return Style.EMPTY.withColor(ChatFormatting.GRAY).withUnderlined(true).withHoverEvent(displayOnHover(text));
    }
}
