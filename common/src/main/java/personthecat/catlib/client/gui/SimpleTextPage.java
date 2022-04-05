package personthecat.catlib.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import personthecat.catlib.config.LibConfig;

import java.util.ArrayList;
import java.util.List;

public class SimpleTextPage extends LibMenu {
    protected final Component details;
    protected final List<FormattedCharSequence> lines;
    public int left;
    public int right;
    public boolean wrap;
    protected int maxScroll;
    protected int scroll;

    public SimpleTextPage(@Nullable Screen parent, Component title, Component details) {
        super(parent, title);
        this.details = details;
        this.lines = new ArrayList<>();
        this.left = 6;
        this.right = 6;
        this.wrap = LibConfig.wrapText();
        this.maxScroll = 0;
        this.scroll = 0;
    }

    @Override
    protected void init() {
        super.init();

        this.resetLines();

        final int menuHeight = this.height - Y1 - Y0;
        final int linesPerPage = menuHeight / (this.font.lineHeight + 1);
        this.maxScroll = this.lines.size() - linesPerPage + 1;

        this.previous.active = false;
        this.next.active = false;
    }

    protected void resetLines() {
        this.lines.clear();
        this.lines.addAll(this.font.split(this.details, this.wrap ? this.width - 12 : 10000));
    }

    @Override
    protected void renderMenu(PoseStack stack, int x, int y, float partial) {
        final int h = this.font.lineHeight + 1;
        int t = Y0 + 6;
        for (int i = this.scroll; i < this.lines.size(); i++) {
            final FormattedCharSequence chars = this.lines.get(i);

            RenderSystem.enableBlend();
            this.font.drawShadow(stack, chars, this.left, t, 0xFFFFFF);
            RenderSystem.disableBlend();

            if ((t += h) > this.height - Y1) {
                return;
            }
        }
    }

    @Override
    protected void renderDetails(PoseStack stack, int x, int y, float partial) {
        super.renderDetails(stack, x, y, partial);
        if (y < Y0 + 6 || y > this.height - Y1 || x < 6 || x > this.width - 6) {
            return;
        }

        final int o = Y0 + 6;
        final int d = y - o;
        final int h = this.font.lineHeight + 1;
        final int l = d / h;
        final int a = this.scroll + l;

        if (a >= this.lines.size()) {
            return;
        }
        final FormattedCharSequence chars = this.lines.get(a);
        final Style s = this.font.getSplitter().componentStyleAtWidth(chars, x - 6);
        if (s == null) {
            return;
        }
        final HoverEvent hover = s.getHoverEvent();
        if (hover == null) {
            return;
        }
        final Component tooltip = hover.getValue(HoverEvent.Action.SHOW_TEXT);
        if (tooltip != null) {
            this.renderTooltip(stack, tooltip, x, y);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        if (d > 0.0) {
            if (this.scroll > 0) {
                this.scroll--;
                return true;
            }
        } else {
            if (this.scroll <= this.maxScroll) {
                this.scroll++;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (super.keyPressed(key, scan, modifiers)) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            if (this.scroll <= this.maxScroll) {
                this.scroll++;
                return true;
            }
        } else if (key == GLFW.GLFW_KEY_UP) {
            if (this.scroll > 0) {
                this.scroll--;
                return true;
            }
        } else if (key == GLFW.GLFW_KEY_PAGE_DOWN) {
            if (this.scroll <= this.maxScroll) {
                this.scroll += 10;
                if (this.scroll > this.maxScroll) {
                    this.scroll = this.maxScroll;
                }
                return true;
            }
        } else if (key == GLFW.GLFW_KEY_PAGE_UP) {
            if (this.scroll > 0) {
                this.scroll -= 10;
                if (this.scroll < 0) {
                    this.scroll = 0;
                }
                return true;
            }
        } else if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_W) {
            this.wrap = !this.wrap;
            this.resetLines();
        }
        return false;
    }
}
