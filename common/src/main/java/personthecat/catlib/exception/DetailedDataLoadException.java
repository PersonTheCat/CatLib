package personthecat.catlib.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.linting.IdLinter;
import personthecat.catlib.serialization.codec.context.DecodeContext;

public class DetailedDataLoadException extends FormattedException {
    private static final String REGISTRY_LOAD_CATEGORY = "catlib.errorMenu.registryLoad";
    private static final String DATA_ENTRY_TITLE_ERROR = "catlib.errorText.dataEntryTitle";
    private static final String DATA_ENTRY_DISPLAY_ERROR = "catlib.errorText.dataEntryDisplay";
    private static final String DATA_ENTRY_DETAIL_ERROR = "catlib.errorText.dataEntryDetail";
    private final DecodeContext ctx;
    private final ResourceKey<?> key;

    public DetailedDataLoadException(DecodeContext ctx, ResourceKey<?> key, Throwable cause) {
        super(cause);
        this.ctx = ctx;
        this.key = key;
    }

    @Override
    public @NotNull String getCategory() {
        return REGISTRY_LOAD_CATEGORY;
    }

    @Override
    public @NotNull Component getTitleMessage() {
        return Component.translatable(DATA_ENTRY_TITLE_ERROR, this.key.location().toString());
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.translatable(DATA_ENTRY_DISPLAY_ERROR, IdLinter.lint(this.key));
    }

    @Override
    public @NotNull Component getDetailMessage() {
        return Component.empty()
            .append(Component.translatable(DATA_ENTRY_DETAIL_ERROR, IdLinter.lint(this.key)))
            .append("\n\n")
            .append(this.ctx.render());
    }
}
