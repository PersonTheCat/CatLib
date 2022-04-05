package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;

import java.util.regex.Pattern;

/**
 * A linter which stylizes resource location arrays in the command output.
 */
public class ResourceArrayLinter extends SyntaxLinter {

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[\\w_-]+(?=:)");
    private static final Pattern PATH_PATTERN = Pattern.compile("[\\w_-]+(?=[,\\]])");

    private static final Highlighter[] HIGHLIGHTERS = {
        new RegexHighlighter(NAMESPACE_PATTERN, color(ChatFormatting.AQUA)),
        new RegexHighlighter(PATH_PATTERN, color(ChatFormatting.GREEN))
    };

    public ResourceArrayLinter() {
        super(HIGHLIGHTERS);
    }
}
