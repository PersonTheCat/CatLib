package personthecat.catlib.util;

import java.util.regex.Pattern;

public class GenericArrayLinter extends SyntaxLinter {

    private static final Pattern ELEMENT_PATTERN = Pattern.compile("[^\\[\\],]+");

    private static final Highlighter[] HIGHLIGHTERS = {
        new RegexHighlighter(ELEMENT_PATTERN, RANDOM_COLOR)
    };

    public GenericArrayLinter() {
        super(HIGHLIGHTERS);
    }
}
