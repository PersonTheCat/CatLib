package personthecat.catlib.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.linting.Linter;
import personthecat.catlib.linting.LinterType;
import personthecat.catlib.linting.Linters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimpleTextPage extends LibMenu {
    protected final MultilineTextBox textBox;

    public SimpleTextPage(@Nullable Screen parent, Component title, Component details) {
        this(parent, title, "", text -> details);
    }

    public SimpleTextPage(@Nullable Screen parent, Component title, String text, Linter highlights) {
        this(parent, title, text, highlights, null);
    }

    public SimpleTextPage(
            @Nullable Screen parent, Component title, String text, Linter highlights, @Nullable Linter details) {
        super(parent, title);
        this.textBox = new MultilineTextBox(text, highlights, details);
    }

    public static SimpleTextPage open(Screen parent, Path path, Component title) throws IOException {
        final var text = Files.readString(path);
        final var highlights = Linters.get(LinterType.HIGHLIGHTS, path);
        final var details = Linters.get(LinterType.DETAILS, path);
        return new SimpleTextPage(parent, title, text, highlights, details);
    }

    @Override
    protected void init() {
        super.init();
        this.textBox.init(0, Y0 + 6, this.width, this.height - Y1 - Y0 - 12, this.font);
        this.setFocused(this.textBox);
        this.addRenderableWidget(this.textBox);
        this.previous.active = false;
        this.next.active = false;
    }

    @Override
    protected void renderDetails(GuiGraphics graphics, int x, int y, float partial) {
        super.renderDetails(graphics, x, y, partial);
        final var tooltip = this.textBox.getTooltipAt(x, y);
        if (tooltip != null) {
            this.setTooltipForNextRenderPass(tooltip);
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        final var style =  this.textBox.getStyleAt((int) x, (int) y);
        if (style != null && style.getClickEvent() != null) {
            if (Screen.hasControlDown() && !this.textBox.hasDraggedAny(x, y) && this.handleComponentClicked(style)) {
                return true;
            }
        }
        return super.mouseReleased(x, y, button);
    }
}
