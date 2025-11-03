package personthecat.catlib.client.gui;

import lombok.extern.log4j.Log4j2;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.function.IOBiConsumer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import personthecat.catlib.linting.Linter;
import personthecat.catlib.linting.LinterType;
import personthecat.catlib.linting.Linters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Log4j2
public class EditableTextPage extends SimpleTextPage {
    private IOBiConsumer<EditableTextPage, String> saveListener;
    private boolean confirmExit;

    public EditableTextPage(@Nullable Screen parent, Component title, String text, Linter highlights) {
        this(parent, title, text, highlights, null);
    }

    public EditableTextPage(
            @Nullable Screen parent, Component title, String text, Linter highlights, @Nullable Linter details) {
        super(parent, title, text, highlights, details);
        this.saveListener = (page, t) -> {};
        this.confirmExit = false;
        this.textBox.setEditable(true);
    }

    public static EditableTextPage open(Screen parent, Path path) throws IOException {
        return open(parent, path, Component.literal(path.getFileName().toString()));
    }

    public static EditableTextPage open(Screen parent, Path path, Component title) throws IOException {
        final var text = Files.readString(path);
        final var highlights = Linters.get(LinterType.HIGHLIGHTS, path, Linters.NONE);
        final var details = Linters.get(LinterType.DETAILS, path, null);
        final var page = new EditableTextPage(parent, title, text, highlights, details);
        page.addSaveListener((p, t) -> {
            Files.writeString(path, t);
            p.toast(Component.translatable("catlib.gui.fileSaved"), Component.translatable("catlib.gui.fileSavedTo", path.getFileName().toString()));
        });
        page.setConfirmExit(true);
        return page;
    }

    public void addSaveListener(IOBiConsumer<EditableTextPage, String> listener) {
        this.saveListener = this.saveListener.andThen(listener);
    }

    public void setConfirmExit(boolean confirmExit) {
        this.confirmExit = confirmExit;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_S) {
            this.saveText();
        }
        return false;
    }

    private void saveText() {
        try {
            this.saveListener.accept(this, this.textBox.getText());
            this.textBox.setDirty(false);
        } catch (final IOException e) {
            log.error("Error saving text output", e);
        }
    }

    @Override
    public void onClose() {
        if (this.confirmExit && this.textBox.isDirty()) {
            Objects.requireNonNull(this.minecraft).setScreen(this.confirmationScreen());
        } else {
            super.onClose();
        }
    }

    private Screen confirmationScreen() {
        return new ConfirmScreen(
            save -> { if (save) this.saveText(); super.onClose(); },
            Component.translatable("catlib.gui.confirmSaveTitle"),
            Component.translatable("catlib.gui.confirmSaveMessage"),
            CommonComponents.GUI_YES,
            CommonComponents.GUI_NO
        );
    }
}
