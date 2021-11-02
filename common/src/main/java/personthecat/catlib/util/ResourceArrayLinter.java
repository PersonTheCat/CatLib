package personthecat.catlib.util;

import net.minecraft.ChatFormatting;
import personthecat.catlib.util.SyntaxLinter;

import java.util.regex.Pattern;

/**
 * A linter which stylizes resource location arrays in the command output.
 */
public class ResourceArrayLinter extends SyntaxLinter {

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[\\w_-]+(?=:)");
    private static final Pattern PATH_PATTERN = Pattern.compile("[\\w_-]+(?=[,\\]])");

    private static final Target[] TARGETS = {
        new Target(NAMESPACE_PATTERN, color(ChatFormatting.AQUA)),
        new Target(PATH_PATTERN, color(ChatFormatting.GREEN))
    };

    public ResourceArrayLinter() {
        super(TARGETS);
    }
}
