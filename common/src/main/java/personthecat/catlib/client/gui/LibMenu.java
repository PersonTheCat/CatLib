package personthecat.catlib.client.gui;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LibMenu extends Screen {

    protected static final int Y0 = 35;
    protected static final int Y1 = 50;

    @Nullable protected Screen parent;
    protected Button previous;
    protected Button cancel;
    protected Button next;

    protected LibMenu(@Nullable Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.children().clear();

        this.previous = Button.builder(CommonComponents.GUI_BACK, b -> this.onPrevious())
            .pos(this.width / 2 - 60 - 120 - 10, this.height - 35)
            .size(120, 20)
            .build();
        this.cancel = Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
            .pos(this.width / 2 - 60, this.height - 35)
            .size(120, 20)
            .build();
        this.next = Button.builder(CommonComponents.GUI_PROCEED, b -> this.onNext())
            .pos(this.width / 2 - 60 + 120 + 10, this.height - 35)
            .size(120, 20)
            .build();

        this.addRenderableWidget(this.previous);
        this.addRenderableWidget(this.cancel);
        this.addRenderableWidget(this.next);
    }

    public LibMenu loadImmediately() {
        final Minecraft minecraft = Minecraft.getInstance();
        final Window window = minecraft.getWindow();
        this.init(minecraft, window.getGuiScaledWidth(), window.getGuiScaledHeight());
        return this;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int x, int y, float partial) {
        super.render(graphics, x, y, partial);
        this.renderMenu(graphics, x, y, partial);
        this.renderDetails(graphics, x, y, partial);
    }

    protected void renderMenu(GuiGraphics graphics, int x, int y, float partial) {}

    protected void renderDetails(GuiGraphics graphics, int x, int y, float partial) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    protected void onPrevious() {}

    protected void onNext() {}
}
