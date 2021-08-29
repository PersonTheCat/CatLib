package personthecat.catlib.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.resources.ResourceLocation;
import org.hjson.JsonValue;
import personthecat.catlib.command.arguments.HjsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.LibReference;

import java.util.concurrent.CompletableFuture;

public class CommandSuggestions {

    /** A suggestion provider suggesting that any value is acceptable. */
    public static final SuggestionProvider<CommandSourceStack> ANY_VALUE =
        register("any_suggestion", (ctx, builder) -> suggestAny(builder));

    /** A suggestion provider suggesting that any integer is acceptable. */
    public static final SuggestionProvider<CommandSourceStack> ANY_INT =
        register("integer_suggestion", "[<integer>]", "-1", "0", "1");

    /** A suggestion provider suggesting that any decimal is acceptable. */
    public static final SuggestionProvider<CommandSourceStack> ANY_DECIMAL =
        register("decimal_suggestion", "[<decimal>]", "0.5", "1.0", "1.5");

    /**
     * A suggestion provider suggesting either the current JSON value or any.
     *
     * <p>Note that this provider requires two previous arguments:</p>
     * <ul>
     *   <li>A {@link HjsonArgument} named <code>file</code></li>
     *   <li>A {@link PathArgument} named <code>path</code></li>
     * </ul>
     * <p>If the current value is missing, "any value" will be suggested.</p>
     */
    public static final SuggestionProvider<CommandSourceStack> CURRENT_JSON =
        register("current_json_suggestion", (ctx, builder) -> {
            final HjsonArgument.Result json = CommandUtils.getLastArg(ctx, HjsonArgument.class, HjsonArgument.Result.class).orElse(null);
            if (json == null) return suggestAny(builder);
            final PathArgument.Result path = CommandUtils.getLastArg(ctx, PathArgument.class, PathArgument.Result.class).orElse(null);
            if (path == null) return suggestAny(builder);
            final JsonValue value = HjsonUtils.getValueFromPath(json.json.get(), path).orElse(null);
            if (value == null) return suggestAny(builder);

            if (value.isObject()) return SharedSuggestionProvider.suggest(new String[] { "{ ... }" }, builder);
            if (value.isArray()) return SharedSuggestionProvider.suggest(new String[] { "[ ... ]" }, builder);
            return SharedSuggestionProvider.suggest(new String[] { value.toString() }, builder);
        });

    private static CompletableFuture<Suggestions> suggestAny(final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(new String[] { "[<any_value>]" }, builder);
    }

    private static SuggestionProvider<CommandSourceStack> register(final String name, final String... suggestions) {
        return register(name, (ctx, builder) -> SharedSuggestionProvider.suggest(suggestions, builder));
    }

    private static SuggestionProvider<CommandSourceStack> register(final String name, final SuggestionProvider<SharedSuggestionProvider> provider) {
        return SuggestionProviders.register(new ResourceLocation(LibReference.MOD_ID, name), provider);
    }

}
