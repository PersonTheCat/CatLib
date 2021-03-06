package personthecat.catlib.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import personthecat.catlib.command.CommandUtils;
import personthecat.catlib.serialization.json.JsonPath;
import personthecat.catlib.serialization.json.XjsUtils;
import personthecat.catlib.util.LibReference;
import xjs.core.JsonObject;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class PathArgument implements ArgumentType<JsonPath> {

    public static void register() {
        ArgumentTypes.register(LibReference.MOD_ID + ":path_argument", PathArgument.class,
            new EmptyArgumentSerializer<>(PathArgument::new));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {
        final Optional<JsonObject> json = CommandUtils.getLastArg(ctx, JsonArgument.class, JsonArgument.Result.class)
            .map(arg -> arg.json.get());
        if (json.isEmpty()) {
            return Suggestions.empty();
        }
        final JsonPath path = CommandUtils.getLastArg(ctx, PathArgument.class, JsonPath.class)
            .orElseGet(() -> new JsonPath(Collections.emptyList()));
        return SharedSuggestionProvider.suggest(XjsUtils.getPaths(json.get(), path), builder);
    }

    @Override
    public JsonPath parse(final StringReader reader) throws CommandSyntaxException {
        return JsonPath.parse(reader);
    }
}