package personthecat.catlib.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import personthecat.catlib.data.MultiValueMap;

import java.util.ArrayList;
import java.util.List;

public class CategorizedList extends ObjectSelectionList<CategorizedList.ListEntry> {

    private final List<ButtonEntry> buttons = new ArrayList<>();

    public CategorizedList(Screen parent, int x0, int x1, MultiValueMap<String, AbstractWidget> widgets) {
        super(Minecraft.getInstance(), x1 - x0, parent.height, 35, parent.height - 50, 22);
        widgets.forEach((category, ws) -> {
            if (category != null && !category.isEmpty()) {
                this.addEntry(new Category(new TranslatableComponent(category)));
            }
            ws.forEach(this::addButton);
        });
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
        this.x0 = x0;
        this.x1 = x1;
    }

    public static Button createButton(Component display, Button.OnPress onPress) {
        return new Button(0, 0, 0, 20, display, onPress);
    }

    public static Button createButton(Component display, Button.OnPress onPress, Button.OnTooltip onTooltip) {
        return new Button(0, 0, 0, 20, display, onPress, onTooltip);
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

    public void renderTooltips(PoseStack stack, int x, int y) {
        final ListEntry entry = this.getEntryAtPosition(x, y);
        if (entry != null) {
            entry.renderTooltip(stack, x, y);
        }
    }

    @Override
    protected int getScrollbarPosition() {
        return this.x1 - 6;
    }

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
        public void render(PoseStack stack, int i, int top, int left, int w, int h, int x, int y, boolean over, float partial) {
            int height = top + (22 / 2) - (this.font.lineHeight / 2);
            drawCenteredString(stack, this.font, this.category, left + (w / 2), height, 16777215);
        }
    }

    static class ButtonEntry extends ListEntry {
        final AbstractWidget widget;

        ButtonEntry(AbstractWidget widget) {
            this.widget = widget;
        }

        @Override
        public void render(PoseStack stack, int i, int top, int left, int w, int h, int x, int y, boolean over, float partial) {
            this.widget.x = left + 4;
            this.widget.y = top;
            this.widget.setWidth(w - 18);
            this.widget.render(stack, x, y, partial);
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
        void renderTooltip(PoseStack stack, int x, int y) {
            this.widget.renderToolTip(stack, x, y);
        }
    }

    public static abstract class ListEntry extends ObjectSelectionList.Entry<ListEntry> {
        void renderTooltip(PoseStack stack, int x, int y) {}
    }
}
