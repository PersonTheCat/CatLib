package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;

/**
 * Legacy highlighters provided for Hjson support. These are presently unused
 * by the library and may be removed at some point.
 */
public class HjsonLinter extends SyntaxLinter {

    public static final Highlighter[] HIGHLIGHTERS = {
        new RegexHighlighter(MULTILINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new RegexHighlighter(LINE_TODO, color(ChatFormatting.YELLOW)),
        new RegexHighlighter(LINE_DOC, color(ChatFormatting.DARK_GREEN).withItalic(true)),
        new RegexHighlighter(MULTILINE_COMMENT, color(ChatFormatting.GRAY)),
        new RegexHighlighter(LINE_COMMENT, color(ChatFormatting.GRAY)),
        new RegexHighlighter(KEY, color(ChatFormatting.AQUA)),
        new RegexHighlighter(BOOLEAN_VALUE, color(ChatFormatting.GOLD)),
        new RegexHighlighter(NUMERIC_VALUE, color(ChatFormatting.LIGHT_PURPLE)),
        new RegexHighlighter(NULL_VALUE, color(ChatFormatting.RED)),
        new RegexHighlighter(BAD_CLOSER, BAD_CLOSER_ERROR),
        UnbalancedTokenHighlighter.INSTANCE
    };

    public HjsonLinter() {
        super(HIGHLIGHTERS);
    }

}
