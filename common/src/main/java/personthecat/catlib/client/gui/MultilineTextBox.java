package personthecat.catlib.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.linting.Linter;
import personthecat.catlib.linting.Linters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MultilineTextBox extends AbstractWidget implements TickableWidget {
    private static final long LINT_DELAY_MS = 200;
    private static final int DRAG_THRESHOLD = 4; // px
    private static final int CURSOR_SCROLL_PAD = 1;

    private final TextBuffer buffer;
    private final List<FormattedCharSequence> lines;
    private Font font;
    private int lineHeight;
    private boolean editable;
    private boolean focused;
    private int scrollX;
    private int scrollY;
    private int maxScrollX;
    private int maxScrollY;
    private long lastBlink;
    private long lastEdit;
    private boolean showCursor;
    private boolean wrap;
    private boolean selecting;
    private boolean dirty;
    private int anchorRow;
    private int anchorCol;
    private int mouseDownX;
    private int mouseDownY;
    private Linter highlights;
    private @Nullable Linter details;

    public MultilineTextBox(String text, Linter highlights, @Nullable Linter details) {
        super(0, 0, 0, 0, Component.empty());
        this.buffer = new TextBuffer();
        this.lines = new ArrayList<>();
        this.font = Minecraft.getInstance().font;
        this.lineHeight = this.font.lineHeight + 2;
        this.focused = false;
        this.scrollX = 0;
        this.scrollY = 0;
        this.lastBlink = System.currentTimeMillis();
        this.showCursor = false;
        this.wrap = LibConfig.wrapText();
        this.selecting = false;
        this.dirty = false;
        this.setHighlights(highlights);
        this.setDetailedLinter(details);
        this.setText(text);
        this.refreshLines();
        this.resetSelection();
    }

    public void init(int x, int y, int width, int height, Font font) {
        this.setX(x);
        this.setY(y);
        this.setWidth(width);
        this.setHeight(height);
        this.font = font;
        this.lineHeight = font.lineHeight + 2;
        this.refreshLines();
    }

    public String getText() {
        return this.buffer.getText();
    }

    public void setText(String text) {
        this.buffer.setText(text);
    }

    public void setHighlights(@Nullable Linter highlights) {
        this.highlights = Objects.requireNonNullElse(highlights, Linters.NONE);
    }

    public void setDetailedLinter(@Nullable Linter details) {
        this.details = details != null ? this.highlights.withOverlay(details) : null;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void refreshLines() {
        this.refreshLines(this.buffer.getText());
    }

    private void refreshLines(String text) {
        if (this.highlights != null) {
            this.refreshLines(this.highlights.lint(text));
        }
    }

    private void refreshLines(Component details) {
        this.lines.clear();
        this.lines.addAll(this.font.split(details, !this.editable && this.wrap ? this.width - 12 : 10_000));
    }

    private void onBufferUpdated() {
        this.refreshLines();
        this.resetSelection();
        this.lastEdit = System.currentTimeMillis();
        this.dirty = true;
    }

    private void resetSelection() {
        this.anchorRow = this.buffer.getCursorRow();
        this.anchorCol = this.buffer.getCursorCol();
    }

    private boolean hasSelection() {
        return this.anchorRow != this.buffer.getCursorRow()
            || this.anchorCol != this.buffer.getCursorCol();
    }

    private int selStartRow() {
        return Math.min(this.buffer.getCursorRow(), this.anchorRow);
    }

    private int selEndRow() {
        return Math.max(this.buffer.getCursorRow(), this.anchorRow);
    }

    private int selStartCol() {
        return Math.min(this.buffer.getCursorCol(), this.anchorCol);
    }

    private int selEndCol() {
        return Math.max(this.buffer.getCursorCol(), this.anchorCol);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        final int visibleLines = this.height / this.lineHeight;
        this.maxScrollY = Math.max(0, this.lines.size() - visibleLines);

        final int visibleWidth = this.width / this.lineHeight * 2;
        this.maxScrollX = Math.max(0, this.buffer.maxCharacterWidth() - visibleWidth);
        final float scrollOffset = (float) (this.scrollX * this.lineHeight / 2);

        RenderSystem.enableBlend();
        graphics.pose().translate(-scrollOffset, 0, 0);

        final int start = this.scrollY;
        final int end = Math.min(this.lines.size(), start + visibleLines);
        final int cursorRow = this.buffer.getCursorRow();
        final int cursorCol = this.buffer.getCursorCol();

        int drawY = this.getY() + 2;
        int drawX = this.getX() + 4;

        this.renderSelectionBackground(graphics, drawX);

        for (int i = start; i < end; i++) {
            final var line = this.lines.get(i);
            graphics.drawString(this.font, line, drawX, drawY, 0xFFFFFF);

            // If editable: draw cursor
            if (this.focused && i == cursorRow && this.showCursor && this.editable) {
                final int cursorX = drawX + this.getWidthAtColumn(line, cursorCol);
                graphics.fill(cursorX, drawY - 1, cursorX + 1, drawY + this.font.lineHeight, 0xFFFFFFFF);
            }
            drawY += this.lineHeight;
        }
        graphics.pose().translate(scrollOffset, 0, 0);
        RenderSystem.disableBlend();
    }

    private void renderSelectionBackground(GuiGraphics graphics, int drawX) {
        if (!this.hasSelection()) return;

        int topRow = Math.min(this.selStartRow(), this.selEndRow());
        int bottomRow = Math.max(this.selStartRow(), this.selEndRow());
        for (int row = topRow; row <= bottomRow; row++) {
            int selStart = (row == this.selStartRow()) ? this.selStartCol() : 0;
            int selEnd = (row == this.selEndRow()) ? this.selEndCol() : this.buffer.getLines().get(row).length();

            selStart = Math.max(0, Math.min(selStart, this.buffer.getLines().get(row).length()));
            selEnd = Math.max(0, Math.min(selEnd, this.buffer.getLines().get(row).length()));

            if (selStart == selEnd) continue;

            int x1 = drawX + this.font.width(this.buffer.getLines().get(row).substring(0, selStart));
            int x2 = drawX + this.font.width(this.buffer.getLines().get(row).substring(0, selEnd));
            int y = this.getY() + (row - this.scrollY) * this.lineHeight;
            graphics.fill(x1, y, x2, y + this.lineHeight, 0x803366FF); // semi-transparent highlight
        }
    }

    public @Nullable Component getTooltipAt(int mouseX, int mouseY) {
        final var style = this.getStyleAt(mouseX, mouseY);
        if (style == null) {
            return null;
        }
        final var hover = style.getHoverEvent();
        if (hover != null) {
            final var tooltip = hover.getValue(HoverEvent.Action.SHOW_TEXT);
            if (tooltip != null) {
                return tooltip;
            }
        }
        final var click = style.getClickEvent();
        if (click != null) {
            return switch (click.getAction()) {
                case OPEN_URL -> Component.translatable("catlib.gui.clickToGo", click.getValue());
                case OPEN_FILE -> Component.translatable("catlib.gui.clickToOpen", Path.of(click.getValue()).getFileName().toString());
                case COPY_TO_CLIPBOARD -> Component.translatable("catlib.gui.clickToCopy", click.getValue());
                case RUN_COMMAND -> Component.translatable("catlib.gui.clickToRun", click.getValue());
                case SUGGEST_COMMAND -> Component.translatable("catlib.gui.clickToSuggest", click.getValue());
                default -> null;
            };
        }
        return null;
    }

    public @Nullable Style getStyleAt(int mouseX, int mouseY) {
        final int row = this.getRowAt(mouseY);
        if (row < 0 || row >= this.lines.size()) {
            return null;
        }
        final var relativeX = this.getRelativeX(mouseX);
        final var line = this.lines.get(row);
        return this.font.getSplitter().componentStyleAtWidth(line, relativeX);
    }

    private int getClampedRowAt(int mouseY) {
        return Math.max(0, Math.min(this.getRowAt(mouseY), this.lines.size() - 1));
    }

    private int getRowAt(int mouseY) {
        final int relativeY = mouseY - this.getY() - 2;
        return relativeY / this.lineHeight + this.scrollY;
    }

    private int getColumnAt(int row, int mouseX) {
        return this.getExactColumnAtWidth(this.lines.get(row), this.getRelativeX(mouseX) + 2);
    }

    private int getRelativeX(int mouseX) {
        final int drawX = this.getX() + 4;
        return mouseX - drawX + (this.scrollX * this.lineHeight / 2);
    }

    private int getExactColumnAtWidth(FormattedCharSequence line, int relativeX) {
        final var sink = new WidthLimitedCharSink(this.font.getSplitter(), relativeX);
        final var col = new MutableInt();
        line.accept((idx, style, codepoint) -> {
            col.increment();
            return sink.accept(idx, style, codepoint);
        });
        return sink.hasRemainingWidth() ? col.getValue() : col.getValue() - 1;
    }

    private int getWidthAtColumn(FormattedCharSequence line, int col) {
        final var accumulator = new WidthAccumulator(this.font.getSplitter(), col);
        line.accept(accumulator);
        return (int) accumulator.getWidth();
    }

    @Override
    public void tick() {
        this.updateDetails();
        this.updateBlink();
    }

    private void updateDetails() {
        if (this.details != null
                && this.details != this.highlights
                && this.lastEdit > 0 && System.currentTimeMillis() - this.lastEdit >= LINT_DELAY_MS) {
            this.refreshLines(this.details.lint(this.buffer.getText()));
            this.lastEdit = 0;
        }
    }

    private void updateBlink() {
        if (this.editable && System.currentTimeMillis() - this.lastBlink > 500) {
            this.showCursor = !this.showCursor;
            this.lastBlink = System.currentTimeMillis();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.isMouseOver(mouseX, mouseY)) return false;

        this.mouseDownX = (int) mouseX;
        this.mouseDownY = (int) mouseY;

        final int row = this.getClampedRowAt((int) mouseY);
        final int col = this.getColumnAt(row, (int) mouseX);
        this.buffer.moveCursorTo(row, col);

        if (!Screen.hasShiftDown()) {
            this.selecting = true;
            this.anchorRow = row;
            this.anchorCol = col;
        }
        this.focused = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!this.selecting) return false;

        final int row = this.getClampedRowAt((int) mouseY);
        final int col = this.getColumnAt(row, (int) mouseX);
        this.anchorRow = row;
        this.anchorCol = col;

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.selecting) return false;

        if (!this.hasDraggedAny(mouseX, mouseY)) {
            this.resetSelection();
        }
        this.selecting = false;
        return true;
    }

    public boolean hasDraggedAny(double mouseX, double mouseY) {
        return Math.abs(this.mouseDownX - mouseX) > DRAG_THRESHOLD
            || Math.abs(this.mouseDownY - mouseY) > DRAG_THRESHOLD;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.focused) return false;

        if (!this.editable) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_UP -> {
                    if (this.scrollY < this.maxScrollY) {
                        this.scrollY++;
                    }
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    if (this.scrollY > 0) {
                        this.scrollY--;
                    }
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    if (this.scrollX < this.maxScrollX) {
                        this.scrollX++;
                    }
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    if (this.scrollX > 0) {
                        this.scrollX--;
                    }
                }
                case GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_W -> {
                    this.wrap = !this.wrap;
                    this.refreshLines();
                }
                default -> { return false; }
            }
        } else if (Screen.hasControlDown()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_C -> this.copySelection();
                case GLFW.GLFW_KEY_V -> this.pasteClipboard();
                case GLFW.GLFW_KEY_X -> this.cutSelection();
                case GLFW.GLFW_KEY_A -> this.selectAll();
                default -> { return false; }
            }
        } else if (Screen.hasShiftDown()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> {
                    this.buffer.moveLeft();
                    this.jumpToCursor();
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    this.buffer.moveRight();
                    this.jumpToCursor();
                }
                case GLFW.GLFW_KEY_UP -> {
                    this.buffer.moveUp();
                    this.jumpToCursor();
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    this.buffer.moveDown();
                    this.jumpToCursor();
                }
                default -> { return false; }
            }
        } else {
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> {
                    this.buffer.moveLeft();
                    this.jumpToCursor();
                    this.resetSelection();
                    this.showCursor = true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    this.buffer.moveRight();
                    this.jumpToCursor();
                    this.resetSelection();
                    this.showCursor = true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    this.buffer.moveUp();
                    this.jumpToCursor();
                    this.resetSelection();
                    this.showCursor = true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    this.buffer.moveDown();
                    this.jumpToCursor();
                    this.resetSelection();
                    this.showCursor = true;
                }
                case GLFW.GLFW_KEY_ENTER -> {
                    this.deleteSelection();
                    this.buffer.insertNewline();
                    this.maxScrollY++;
                    this.jumpToCursor();
                    this.onBufferUpdated();
                }
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (!this.deleteSelection()) {
                        this.buffer.backspace();
                    }
                    this.jumpToCursor();
                    this.onBufferUpdated();
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    this.buffer.delete();
                    this.onBufferUpdated();
                }
                default -> { return false; }
            }
        }
        return true;
    }

    private boolean deleteSelection() {
        if (this.hasSelection()) {
            this.buffer.deleteSection(this.selStartRow(), this.selEndRow(), this.selStartCol(), this.selEndCol());
            return true;
        }
        return false;
    }

    private void copySelection() {
        if (!this.hasSelection()) return;

        final var kbHandler = Minecraft.getInstance().keyboardHandler;
        final var text = this.buffer.getSection(this.selStartRow(), this.selEndRow(), this.selStartCol(), this.selEndCol());
        kbHandler.setClipboard(text);
    }

    private void pasteClipboard() {
        final var kbHandler = Minecraft.getInstance().keyboardHandler;
        final var text = kbHandler.getClipboard();
        this.deleteSelection();
        this.buffer.insertText(text);
        this.onBufferUpdated();
    }

    private void cutSelection() {
        if (!this.hasSelection()) return;

        final var kbHandler = Minecraft.getInstance().keyboardHandler;
        final var text = this.buffer.getSection(this.selStartRow(), this.selEndRow(), this.selStartCol(), this.selEndCol());
        this.buffer.deleteSection(this.selStartRow(), this.selEndRow(), this.selStartCol(), this.selEndCol());
        kbHandler.setClipboard(text);
        this.onBufferUpdated();
    }

    private void selectAll() {
        this.buffer.moveCursorTo(0, 0);
        this.anchorRow = this.buffer.lineCount();
        this.anchorCol = this.buffer.getLine(this.anchorRow - 1).length();
    }

    // this still uses heuristics. We can update the horizontal scroll later
    private void jumpToCursor() {
        if (this.scrollY > this.buffer.getCursorRow() - CURSOR_SCROLL_PAD) {
            this.scrollY = Math.max(0, this.buffer.getCursorRow() - CURSOR_SCROLL_PAD);
        } else {
            final var endOfScreen = this.buffer.getCursorRow() - (this.height / this.lineHeight) + 1;
            if (this.scrollY < endOfScreen + CURSOR_SCROLL_PAD) {
                this.scrollY = Math.min(this.maxScrollY, endOfScreen + CURSOR_SCROLL_PAD);
            }
        }
        if (this.scrollX > this.buffer.getCursorCol() - CURSOR_SCROLL_PAD) {
            this.scrollX = Math.max(0, this.buffer.getCursorCol() - CURSOR_SCROLL_PAD);
        } else {
            final var endOfScreen = this.buffer.getCursorCol() - (this.width / (this.lineHeight / 2)) + 1;
            if (this.scrollX < endOfScreen + CURSOR_SCROLL_PAD) {
                this.scrollX = Math.min(this.maxScrollX, endOfScreen + CURSOR_SCROLL_PAD);
            }
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.focused || !this.editable) return false;

        if (codePoint >= 32 && codePoint != 127) { // ignore control chars
            this.deleteSelection();
            this.buffer.insertChar(codePoint);
            this.onBufferUpdated();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!this.visible) return false;
        if (!this.isMouseOver(mouseX, mouseY)) return false;

        final int dY = (int) Math.signum(-deltaY);
        this.scrollY = Mth.clamp(this.scrollY + dY, 0, this.maxScrollY);

        final int dX = (int) Math.signum(-deltaX);
        this.scrollX = Mth.clamp(this.scrollX + dX, 0, this.maxScrollX);

        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        this.focused = focused;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    @Override
    public boolean isFocused() {
        return this.focused;
    }
}
