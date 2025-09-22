package personthecat.catlib.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.CatLib;
import personthecat.catlib.command.arguments.JsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.command.arguments.RegistryArgument;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.serialization.json.JsonPath;
import personthecat.catlib.serialization.json.XjsUtils;
import personthecat.catlib.util.LibUtil;
import xjs.data.JsonValue;

import java.util.concurrent.CompletableFuture;

public class LibSuggestions {

    /**
     * A suggestion provider suggesting either the current JSON value or any.
     *
     * <p>Note that this provider requires two previous arguments:</p>
     * <ul>
     *   <li>A {@link JsonArgument} named <code>file</code></li>
     *   <li>A {@link PathArgument} named <code>path</code></li>
     * </ul>
     * <p>If the current value is missing, "any value" will be suggested.</p>
     */
    public static final SuggestionProvider<CommandSourceStack> CURRENT_JSON =
        register("current_json_suggestion", (ctx, builder) -> {
            final JsonArgument.Result json = CommandUtils.getLastArg(ctx, JsonArgument.class, JsonArgument.Result.class).orElse(null);
            if (json == null) return suggestJson(builder);
            final JsonPath path = CommandUtils.getLastArg(ctx, PathArgument.class, JsonPath.class).orElse(null);
            if (path == null) return suggestJson(builder);
            final JsonValue value = XjsUtils.getValueFromPath(json.json.get(), path).orElse(null);
            if (value == null) return suggestJson(builder);

            if (value.isObject()) return SharedSuggestionProvider.suggest(new String[] { "{ ... }" }, builder);
            if (value.isArray()) return SharedSuggestionProvider.suggest(new String[] { "[ ... ]" }, builder);
            return SharedSuggestionProvider.suggest(new String[] { value.toString() }, builder);
        });

    /**
     * A suggestion provider suggesting the ids of the registry from the
     * previous argument.
     */
    public static final SuggestionProvider<CommandSourceStack> PREVIOUS_IDS =
        register("previous_ids_suggestion", (ctx, builder) -> {
            final Class<? extends RegistryArgument<?>> type = LibUtil.asParentType(RegistryArgument.class);
            final RegistryHandle<?> handle = CommandUtils.getLastArg(ctx, type, RegistryHandle.class).orElse(null);
            if (handle == null) return Suggestions.empty();
            return SharedSuggestionProvider.suggest(handle.keySet().stream().map(Object::toString), builder);
        });

    private static CompletableFuture<Suggestions> suggestJson(final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(new String[] { "[<json_data>]" }, builder);
    }

    private static SuggestionProvider<CommandSourceStack> register(final String name, final String... suggestions) {
        return register(name, (ctx, builder) -> SharedSuggestionProvider.suggest(suggestions, builder));
    }

    private static SuggestionProvider<CommandSourceStack> register(final String name, final SuggestionProvider<SharedSuggestionProvider> provider) {
        return SuggestionProviders.register(new ResourceLocation(CatLib.ID, name), provider);
    }

}
