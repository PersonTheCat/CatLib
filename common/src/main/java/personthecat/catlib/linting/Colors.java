package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

import java.util.Random;
import java.util.function.Supplier;

final class Colors {
    static final Style STYLE = Style.EMPTY.applyFormats();

    static final Style[] COLORS = {
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

    static Supplier<Style> getter(final Style style) {
        if (style != STYLE) return () -> style;
        final var rand = new Random(1234); // not supposed to be random, but a sequence
        return () -> COLORS[rand.nextInt() % COLORS.length];
    }
}
