package personthecat.catlib.exception;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
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
        return Component.translatable(DATA_ENTRY_TITLE_ERROR, this.key.location());
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.translatable(DATA_ENTRY_DISPLAY_ERROR, this.formatKey());
    }

    @Override
    public @NotNull Component getDetailMessage() {
        return Component.empty()
            .append(Component.translatable(DATA_ENTRY_DETAIL_ERROR, this.formatKey()))
            .append("\n\n")
            .append(this.ctx.render());
    }

    private Component formatKey() {
        return Component.empty()
            .append(this.formatId(this.key.registry()))
            .append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY))
            .append(this.formatId(this.key.location()));
    }

    private Component formatId(ResourceLocation id) {
        return Component.empty()
            .append(Component.literal(id.getNamespace()).withStyle(ChatFormatting.AQUA))
            .append(":")
            .append(Component.literal(id.getPath()).withStyle(ChatFormatting.GREEN));
    }
}
