package personthecat.catlib.util;

import java.util.regex.Pattern;

public class GenericArrayLinter extends SyntaxLinter {

    private static final Pattern ELEMENT_PATTERN = Pattern.compile("[^\\[\\],\\s]");

    private static final Target[] TARGETS = {
        new Target(ELEMENT_PATTERN, RANDOM_COLOR)
    };

    public GenericArrayLinter() {
        super(TARGETS);
    }
}
