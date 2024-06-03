package personthecat.catlib.config;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.exception.FormattedException;

public class ValidationException extends FormattedException {
    private final String filename;
    private final String field;
    private final Component error;
    private final Component details;

    public ValidationException(String filename, String field, String msg) {
        this(filename, field, Component.translatable(msg, field));
    }

    public ValidationException(String filename, String field, Component msg) {
        this(filename, field, msg, msg);
    }

    public ValidationException(String filename, String field, Component error, Component details) {
        super(field + " in " + filename);
        this.filename = filename;
        this.field = field;
        this.error = error;
        this.details = details;
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
        return this.details;
    }

    @Override
    public @Nullable Component getTooltip() {
        return this.error;
    }
}
