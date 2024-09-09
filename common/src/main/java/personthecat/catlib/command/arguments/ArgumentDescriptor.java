package personthecat.catlib.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

public record ArgumentDescriptor<T>(
        ArgumentType<T> type, @Nullable SuggestionProvider<CommandSourceStack> suggestions) {
    public static final ArgumentDescriptor<Void> LITERAL = new ArgumentDescriptor<>(null);

    public ArgumentDescriptor(final ArgumentType<T> type) {
        this(type, null);
    }

    public boolean isLiteral() {
        return this.type == null;
    }
}
