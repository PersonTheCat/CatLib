package personthecat.catlib.linting;

import java.util.regex.Pattern;

/**
 * A linter which stylizes resource location arrays in the command output.
 */
public final class IdListLinter {
    private static final Pattern ID_PATTERN = Pattern.compile("\\w+:\\w+", Pattern.MULTILINE);

    public static final Linter INSTANCE = Linter.of(
        RegexHighlighter.builder(ID_PATTERN).linter(IdLinter.ID).build());

    private IdListLinter() {}
}
