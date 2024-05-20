package personthecat.catlib.exception;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GenericFormattedException extends FormattedException {
    private final Component tooltip;

    public GenericFormattedException(final Throwable cause) {
        super(cause);
        this.tooltip = null;
    }

    public GenericFormattedException(final Throwable cause, final String tooltip) {
        super(cause);
        this.tooltip = Component.literal(tooltip);
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.literal(createMsg(this.getCause()));
    }

    @Override
    public @Nullable Component getTooltip() {
        return this.tooltip;
    }
}
