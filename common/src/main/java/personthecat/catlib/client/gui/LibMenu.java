package personthecat.catlib.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LibMenu extends Screen {

    protected static final float G = 32.0F;
    protected static final int Y0 = 35;
    protected static final int Y1 = 50;

    @Nullable protected Screen parent;
    protected Font font;
    protected Button previous;
    protected Button cancel;
    protected Button next;

    protected LibMenu(@Nullable Screen parent, Component title) {
        super(title);
        this.font = Minecraft.getInstance().font;
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.children().clear();

        this.previous = new Button(this.width / 2 - 60 - 120 - 10, this.height - 35, 120, 20,
            CommonComponents.GUI_BACK, b -> this.onPrevious());
        this.cancel = new Button(this.width / 2 - 60,  this.height - 35, 120, 20,
            CommonComponents.GUI_CANCEL, b -> this.onClose());
        this.next = new Button(this.width / 2 - 60 + 120 + 10, this.height - 35, 120, 20,
            CommonComponents.GUI_PROCEED, b -> this.onNext());

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

    boolean menu = true;

    @Override
    public void render(@NotNull PoseStack stack, int x, int y, float partial) {
        this.renderBackground(stack);

        final Tesselator tx = Tesselator.getInstance();
        final BufferBuilder builder = tx.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        this.renderBackground(tx, builder);
        if (menu) {
            this.renderMenu(stack, x, y, partial);
        }
        this.renderTopAndBottom(tx, builder);
        this.renderDetails(stack, x, y, partial);

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    boolean bgConfig = true, bg = true;

    protected void renderBackground(Tesselator tx, BufferBuilder builder) {
        if (bgConfig) {
            RenderSystem.setShaderTexture(0, GuiComponent.BACKGROUND_LOCATION);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (bg) {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            builder.vertex(0.0, this.height - Y1, 0.0D).uv(0.0F, (this.height - Y1) / G).color(32, 32, 32, 255).endVertex();
            builder.vertex(this.width, this.height - Y1, 0.0D).uv((float) this.width / G, (float) (this.height - Y1) / G).color(32, 32, 32, 255).endVertex();
            builder.vertex(this.width, Y0, 0.0D).uv(this.width / G, (float) Y0 / G).color(32, 32, 32, 255).endVertex();
            builder.vertex(0.0, Y0, 0.0D).uv(0.0F, Y0 / G).color(32, 32, 32, 255).endVertex();
            tx.end();
        }
    }

    protected void renderMenu(PoseStack stack, int x, int y, float partial) {}

    boolean topConfig = true,
            top = true,
            bottomConfig = true,
            bottom = true,
    forceBlack = true;

    protected void renderTopAndBottom(Tesselator tx, BufferBuilder builder) {
        if (topConfig) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, GuiComponent.BACKGROUND_LOCATION);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(519); // GL_ALWAYS
        }

        if (top) { // top and bottom
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            builder.vertex(0.0, Y0, -100.0D).uv(0.0F, Y0 / G).color(64, 64, 64, 255).endVertex();
            builder.vertex(this.width, Y0, -100.0D).uv((float)this.width / G, Y0 / G).color(64, 64, 64, 255).endVertex();
            builder.vertex(this.width, 0.0D, -100.0D).uv((float)this.width / G, 0.0F).color(64, 64, 64, 255).endVertex();
            builder.vertex(0.0, 0.0D, -100.0D).uv(0.0F, 0.0F).color(64, 64, 64, 255).endVertex();
            builder.vertex(0.0, this.height, -100.0D).uv(0.0F, (float)this.height / G).color(64, 64, 64, 255).endVertex();
            builder.vertex(this.width, this.height, -100.0D).uv((float)this.width / G, (float)this.height / G).color(64, 64, 64, 255).endVertex();
            builder.vertex(this.width, this.height - Y1, -100.0D).uv((float)this.width / G, (this.height - Y1) / G).color(64, 64, 64, 255).endVertex();
            builder.vertex(0.0, this.height - Y1, -100.0D).uv(0.0F, (this.height - Y1) / G).color(64, 64, 64, 255).endVertex();
            tx.end();
        }

        if (bottomConfig) {
            RenderSystem.depthFunc(515); // GL_EQUAL
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            RenderSystem.disableTexture();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
        }

        if (forceBlack) {
            RenderSystem.setShaderColor(0, 0, 0, 1);
        }

        if (bottom) { // shadows
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            builder.vertex(0.0, Y0 + 4, 0.0D).uv(0.0F, 1.0F).color(0, 0, 0, 0).endVertex();
            builder.vertex(this.width, Y0 + 4, 0.0D).uv(1.0F, 1.0F).color(0, 0, 0, 0).endVertex();
            builder.vertex(this.width, Y0, 0.0D).uv(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
            builder.vertex(0.0, Y0, 0.0D).uv(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
            builder.vertex(0.0, this.height - Y1, 0.0D).uv(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
            builder.vertex(this.width, this.height - Y1, 0.0D).uv(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
            builder.vertex(this.width, this.height - Y1 - 4, 0.0D).uv(1.0F, 0.0F).color(0, 0, 0, 0).endVertex();
            builder.vertex(0.0, this.height - Y1 - 4, 0.0D).uv(0.0F, 0.0F).color(0, 0, 0, 0).endVertex();
            tx.end();
        }
    }

    protected void renderDetails(PoseStack stack, int x, int y, float partial) {
        drawCenteredString(stack, this.font, this.title, this.width / 2, 20, 16777215);
        super.render(stack, x, y, partial);
    }

    @Override
    public void renderTooltip(@NotNull PoseStack stack, @NotNull Component tooltip, int x, int y) {
        this.renderTooltip(stack, this.font.split(tooltip,this.width / 2), x, y);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    protected void onPrevious() {}

    protected void onNext() {}
}
