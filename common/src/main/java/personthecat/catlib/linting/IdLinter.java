package personthecat.catlib.linting;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class IdLinter {
    public static final Linter ID = text -> lint(new ResourceLocation(text));

    private IdLinter() {}

    public static Component lint(ResourceLocation id) {
        return Component.empty()
            .append(Component.literal(id.getNamespace()).withStyle(ChatFormatting.AQUA))
            .append(":")
            .append(Component.literal(id.getPath()).withStyle(ChatFormatting.GREEN));
    }

    public static Component lint(ResourceKey<?> key) {
        return Component.empty()
            .append(lint(key.registry()))
            .append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY))
            .append(lint(key.location()));
    }
}
