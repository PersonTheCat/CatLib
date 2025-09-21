package personthecat.catlib.config;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.exception.FormattedException;

public class ValueException extends FormattedException {
    private static final String GENERIC_ERROR = "catlib.errorText.configValue";

    private final String filename;
    private final ConfigValue value;

    public ValueException(String msg, String filename, ConfigValue value) {
        this(msg, filename, value, null);
    }

    public ValueException(String msg, String filename, ConfigValue value, @Nullable Throwable cause) {
        super(msg, cause);
        this.filename = filename;
        this.value = value;
    }

    @Override
    public @NotNull String getCategory() {
        return this.filename;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.literal(this.value.name());
    }

    @Override
    public @Nullable Component getTooltip() {
        return Component.translatable(this.getMessage(), this.value.name());
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return Component.translatable(GENERIC_ERROR, this.value.name());
    }
}