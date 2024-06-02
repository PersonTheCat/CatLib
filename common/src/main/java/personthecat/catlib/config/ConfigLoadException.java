package personthecat.catlib.config;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.exception.FormattedException;

public class ConfigLoadException extends FormattedException {
    private static final String CONFIG_CATEGORY = "catlib.errorMenu.config";
    private static final String GENERIC_ERROR = "catlib.errorText.configError";

    private final ModDescriptor mod;
    private final String filename;

    public ConfigLoadException(ModDescriptor mod, String filename, Throwable cause) {
        super(cause);
        this.mod = mod;
        this.filename = filename;
    }

    @Override
    public @NotNull String getCategory() {
        return CONFIG_CATEGORY;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.literal(this.filename);
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return Component.translatable(GENERIC_ERROR, this.mod.getModId());
    }
}
