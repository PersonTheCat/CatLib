package personthecat.catlib.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.util.LibReference;

public class CommandSuggestions {

    /** A suggestion provider suggesting that any value is acceptable. */
    public static final SuggestionProvider<CommandSourceStack> ANY_VALUE = createAnySuggestion();

    public static final SuggestionProvider<CommandSourceStack> ANY_INT = createIntegerSuggestion();

    public static final SuggestionProvider<CommandSourceStack> ANY_DECIMAL = createDecimalSuggestion();

    /** Generates the "any value" suggestion provider. */
    private static SuggestionProvider<CommandSourceStack> createAnySuggestion() {
        return register("any_suggestion", "[<any_value>]");
    }

    /** Generates the integer suggestion provider. */
    private static SuggestionProvider<CommandSourceStack> createIntegerSuggestion() {
        return register("integer_suggestion", "[<integer>]", "-1", "0", "1");
    }

    /** Generates the decimal suggestion provider. */
    private static SuggestionProvider<CommandSourceStack> createDecimalSuggestion() {
        return register("decimal_suggestion", "[<decimal>]", "0.5", "1.0", "1.5");
    }

    private static SuggestionProvider<CommandSourceStack> register(final String name, final String... suggestions) {
        return register(name, (ctx, builder) -> SharedSuggestionProvider.suggest(suggestions, builder));
    }

    private static SuggestionProvider<CommandSourceStack> register(final String name, final SuggestionProvider<SharedSuggestionProvider> provider) {
        return SuggestionProviders.register(new ResourceLocation(LibReference.MOD_ID, name), provider);
    }

}