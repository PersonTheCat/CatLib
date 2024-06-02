package personthecat.catlib.config;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.exception.FormattedException;

public class ValidationException extends FormattedException {
    private final String filename;
    private final String field;
    private final @Nullable Object value;

    public ValidationException(String filename, String field, String msg) {
        this(filename, field, msg, null);
    }

    public ValidationException(String filename, String field, String msg, @Nullable Object value) {
        super(msg);
        this.filename = filename;
        this.field = field;
        this.value = value;
    }

    public ValidationException(String filename, String field, String msg, Throwable cause) {
        super(msg, cause);
        this.filename = filename;
        this.field = field;
        this.value = null;
    }

    @Override
    public @NotNull String getCategory() {
        return this.filename;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.literal(this.field);
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return this.value != null ? Component.translatable(this.getMessage(), this.value) : super.getTitleMessage();
    }

    @Override
    public @Nullable Component getTooltip() {
        return Component.translatable(this.getMessage(), this.field);
    }
}
