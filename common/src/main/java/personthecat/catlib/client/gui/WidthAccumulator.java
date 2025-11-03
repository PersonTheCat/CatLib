package personthecat.catlib.client.gui;

import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import personthecat.catlib.mixin.StringSplitterAccessor;

class WidthAccumulator implements FormattedCharSink {
    private final StringSplitter.WidthProvider widthProvider;
    private int maxIdx;
    private float width;

    WidthAccumulator(StringSplitter splitter, int maxIdx) {
        this.widthProvider = ((StringSplitterAccessor) splitter).getWidthProvider();
        this.maxIdx = maxIdx;
        this.width = 0;
    }

    @Override
    public boolean accept(int idx, Style style, int codepoint) {
        if (this.maxIdx-- > 0) {
            this.width += this.widthProvider.getWidth(codepoint, style);
            return true;
        }
        return false;
    }

    float getWidth() {
        return this.width;
    }
}
