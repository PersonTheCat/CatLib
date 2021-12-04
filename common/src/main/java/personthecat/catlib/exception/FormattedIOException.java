package personthecat.catlib.exception;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.util.LibStringUtils;
import personthecat.catlib.util.PathUtils;

import java.io.File;

public class FormattedIOException extends FormattedException {

    final File root;
    final File file;
    final String relative;
    final String msg;

    public FormattedIOException(final File file, final Throwable cause) {
        this(file.getParentFile(), file, cause);
    }

    public FormattedIOException(final File file, final Throwable cause, final String msg) {
        this(file.getParentFile(), file, cause, msg);
    }

    public FormattedIOException(final File root, final File file, final Throwable cause) {
        this(root, file, cause, createMsg(cause));
    }

    public FormattedIOException(final File root, final File file, final Throwable cause, final String msg) {
        super(msg, cause);
        this.root = root;
        this.file = file;
        this.relative = PathUtils.getRelativePath(root, file);
        this.msg = msg;
    }

    private static String createMsg(final Throwable cause) {
        final String m = cause.getMessage();
        return m != null ? m : LibStringUtils.toTitleCase(cause.getClass().getSimpleName());
    }

    @Override
    public @NotNull String getCategory() {
        return "catlib.errorMenu.io";
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return new TextComponent(this.relative);
    }

    @Override
    public @Nullable Component getTooltip() {
        return new TranslatableComponent(this.msg);
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return new TranslatableComponent("catlib.errorText.fileError", this.relative);
    }

    @Override
    public @NotNull Component getDetailMessage() {
        final Component newLine = new TextComponent("\n");
        final MutableComponent component = new TextComponent("");

        final Style title = Style.EMPTY.applyFormats(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);

        component.append(new TranslatableComponent("catlib.errorText.fileDiagnostics").withStyle(title));
        component.append(newLine);
        component.append(newLine);

        final Component bullet = new TextComponent(" * ").withStyle(Style.EMPTY.withBold(true));
        final Component space = new TextComponent(" ");
        final Style purple = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
        final Style orange = Style.EMPTY.withColor(ChatFormatting.GOLD);
        final Style green = Style.EMPTY.withColor(ChatFormatting.GREEN);
        final Style blue = Style.EMPTY.withColor(ChatFormatting.DARK_BLUE);
        final Style red = Style.EMPTY.withColor(ChatFormatting.RED);

        try {
            final boolean exists = this.file.exists();
            component.append(bullet);
            component.append(new TranslatableComponent("catlib.errorText.fileExists").withStyle(purple));
            component.append(space);
            component.append(new TextComponent(String.valueOf(exists)).withStyle(orange));
            component.append(newLine);

            final boolean dirExists = this.file.getParentFile().exists();
            component.append(bullet);
            component.append(new TranslatableComponent("catlib.errorText.directoryExists").withStyle(purple));
            component.append(space);
            component.append(new TextComponent(String.valueOf(dirExists)).withStyle(orange));
            component.append(newLine);

            final String type = PathUtils.extension(this.file);
            component.append(bullet);
            component.append(new TranslatableComponent("catlib.errorText.fileType").withStyle(purple));
            component.append(space);
            component.append(new TextComponent(type).withStyle(green));
            component.append(newLine);

            final long kb = this.file.length() / 1000;
            component.append(bullet);
            component.append(new TranslatableComponent("catlib.errorText.fileSize").withStyle(purple));
            component.append(space);
            component.append(new TextComponent(kb + "kb").withStyle(blue));
            component.append(newLine);
        } catch (final SecurityException ignored) {
            component.append(new TranslatableComponent("catlib.errorText.ioNotAllowed").withStyle(red));
            component.append(newLine);
        }

        component.append(newLine);
        component.append(new TranslatableComponent("catlib.errorText.stackTrace").withStyle(title));
        component.append(newLine);
        component.append(newLine);

        component.append(super.getDetailMessage());

        return component;
    }
}
