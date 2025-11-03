package personthecat.catlib.client.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class LibMenu extends Screen {
    protected static final ResourceLocation MENU_LIST_BACKGROUND = new ResourceLocation("textures/gui/menu_list_background.png");

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

    @Override
    public void tick() {
        for (final var widget : this.children()) {
            if (widget instanceof TickableWidget tickable) {
                tickable.tick();
            }
        }
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

    @Override
    public void renderBackground(GuiGraphics graphics, int x, int z, float partial) {
        super.renderBackground(graphics, x, z, partial);
        this.renderBorder(graphics);
    }

    @Override
    protected void renderMenuBackground(GuiGraphics graphics) {
        RenderSystem.enableBlend();
        graphics.blit(MENU_LIST_BACKGROUND, 0, Y0, this.width, this.menuHeight(), this.width, this.menuHeight(), 32, 32);
        RenderSystem.disableBlend();
    }

    protected void renderBorder(GuiGraphics graphics) {
        RenderSystem.enableBlend();
        graphics.blit(HEADER_SEPARATOR, 0, Y0 - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
        graphics.blit(FOOTER_SEPARATOR, 0, this.height - Y1, 0.0F, 0.0F, this.width, 2, 32, 2);
        RenderSystem.disableBlend();
    }

    protected int menuHeight() {
        return this.height - Y0 - Y1;
    }

    protected void renderMenu(GuiGraphics graphics, int x, int y, float partial) {}

    protected void renderDetails(GuiGraphics graphics, int x, int y, float partial) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
    }

    public void toast(Component title, Component message) {
        final var mc = Objects.requireNonNull(this.minecraft);
        mc.getToasts().addToast(SystemToast.multiline(mc, new SystemToast.SystemToastId(), title, message));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    protected void onPrevious() {}

    protected void onNext() {}
}
