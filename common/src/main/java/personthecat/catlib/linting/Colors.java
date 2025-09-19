package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

final class Colors {
    public static final Style STYLE = Style.EMPTY.applyFormats();

    private static final Style[] COLORS = {
        color(ChatFormatting.YELLOW),
        color(ChatFormatting.GREEN),
        color(ChatFormatting.AQUA),
        color(ChatFormatting.GOLD),
        color(ChatFormatting.BLUE),
        color(ChatFormatting.LIGHT_PURPLE),
        color(ChatFormatting.RED)
    };

    private Colors() {}

    static Style color(final ChatFormatting color) {
        return Style.EMPTY.withColor(color);
    }

    static Style get(final Style style, int idx) {
        return style != STYLE ? style : COLORS[idx % COLORS.length];
    }
}
