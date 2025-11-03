package personthecat.catlib.client.gui;

import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Style;
import personthecat.catlib.mixin.StringSplitterAccessor;

// private utility in StringSplitter that is otherwise difficult to acquire
class WidthLimitedCharSink {
    private final StringSplitter.WidthProvider widthProvider;
    private float maxWidth;

    WidthLimitedCharSink(StringSplitter splitter, float maxWidth) {
        this.widthProvider = ((StringSplitterAccessor) splitter).getWidthProvider();
        this.maxWidth = maxWidth;
    }

    boolean accept(int idx, Style style, int codepoint) {
        this.maxWidth -= this.widthProvider.getWidth(codepoint, style);
        return this.hasRemainingWidth();
    }

    boolean hasRemainingWidth() {
        return this.maxWidth >= 0.0F;
    }
}
