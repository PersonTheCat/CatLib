package personthecat.catlib.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

@Value
@AllArgsConstructor
public class ArgumentDescriptor<T> {
    public static final ArgumentDescriptor<Void> LITERAL = new ArgumentDescriptor<>(null);

    ArgumentType<T> type;
    @Nullable SuggestionProvider<CommandSourceStack> suggestions;

    public ArgumentDescriptor(final ArgumentType<T> type) {
        this(type, null);
    }

    public boolean isLiteral() {
        return this.type == null;
    }
}
