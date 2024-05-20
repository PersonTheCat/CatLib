package personthecat.catlib.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.collections.MultiValueMap;

import java.util.ArrayList;
import java.util.List;

public class CategorizedList extends ObjectSelectionList<CategorizedList.ListEntry>{

    private static final int BORDER = 35;
    private static final int PAD = 15;
    private final List<ButtonEntry> buttons = new ArrayList<>();

    // i = width, j = height, k = y, 0 = x
    // in order: mc, width, height, y-offset, entryHeight
    public CategorizedList(Screen parent, int x0, int x1, MultiValueMap<String, AbstractWidget> widgets) {
        super(Minecraft.getInstance(), 0, 0, 0, 22);
        widgets.forEach((category, ws) -> {
            if (category != null && !category.isEmpty()) {
                this.addEntry(new Category(Component.translatable(category)));
            }
            ws.forEach(this::addButton);
        });
        this.setX(x0);
        this.setY(BORDER);
        this.width = x1 - x0;
        this.height = parent.height - BORDER - PAD;
    }

    public static Button createButton(Component display, Button.OnPress onPress) {
        return Button.builder(display, onPress).build();
    }

    public static Button createButton(Component display, Button.OnPress onPress, Tooltip tooltip) {
        return Button.builder(display, onPress).tooltip(tooltip).build();
    }

    private void addButton(AbstractWidget widget) {
        final ButtonEntry entry = new ButtonEntry(widget);
        this.addEntry(entry);
        this.buttons.add(entry);
    }

    public int numButtons() {
        return this.buttons.size();
    }

    public void selectButton(int button) {
        this.buttons.get(button).widget.active = false;
    }

    public void deselectAll() {
        this.buttons.forEach(b -> b.widget.active = true);
    }

    public AbstractWidget getButton(int button) {
        return this.buttons.get(button).widget;
    }

    public @Nullable Tooltip getTooltipAtPosition(int x, int y) {
        final ListEntry entry = this.getEntryAtPosition(x, y);
        return entry != null ? entry.getTooltip() : null;
    }

    @Override
    protected int getScrollbarPosition() {
        return (this.width + this.getX()) - 6;
    }

    @Override
    protected void renderListBackground(GuiGraphics graphics) {}

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {}

    @Override
    public int getRowWidth() {
        return this.width;
    }

    static class Category extends ListEntry {
        private final Component category;
        private final Font font = Minecraft.getInstance().font;

        Category(Component category) {
            this.category = category;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int i, int top, int left, int w, int h, int x, int y, boolean over, float partial) {
            int height = top + (22 / 2) - (this.font.lineHeight / 2);
            graphics.drawCenteredString(this.font, this.category, left + (w / 2), height, 16777215);
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.category);
        }
    }

    static class ButtonEntry extends ListEntry {
        final AbstractWidget widget;

        ButtonEntry(AbstractWidget widget) {
            this.widget = widget;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int i, int top, int left, int w, int h, int x, int y, boolean over, float partial) {
            this.widget.setX(left + 4);
            this.widget.setY(top);
            this.widget.setWidth(w - 18);
            this.widget.render(graphics, x, y, partial);
        }

        @Override
        public boolean mouseClicked(double x, double y, int button) {
            return this.widget.mouseClicked(x, y, button);
        }

        @Override
        public boolean mouseReleased(double x, double y, int button) {
            return this.widget.mouseReleased(x, y, button);
        }

        @Override
        public @Nullable Tooltip getTooltip() {
            return this.widget.getTooltip();
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.widget.getMessage());
        }
    }

    public static abstract class ListEntry extends ObjectSelectionList.Entry<ListEntry> {
        public @Nullable Tooltip getTooltip() {
            return null;
        }
    }
}
