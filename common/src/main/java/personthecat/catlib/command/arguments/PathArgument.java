package personthecat.catlib.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import personthecat.catlib.command.CommandUtils;
import personthecat.catlib.serialization.json.JsonPath;
import personthecat.catlib.serialization.json.XjsUtils;
import xjs.data.JsonObject;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PathArgument implements ArgumentType<JsonPath> {

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {
        final Optional<JsonObject> json = CommandUtils.getLastArg(ctx, JsonArgument.class, JsonArgument.Result.class)
            .map(arg -> arg.json.get());
        if (json.isEmpty()) {
            return Suggestions.empty();
        }
        final JsonPath path = CommandUtils.getLastArg(ctx, PathArgument.class, JsonPath.class)
            .orElseGet(() -> new JsonPath(Collections.emptyList()));
        return SharedSuggestionProvider.suggest(XjsUtils.getPaths(json.get(), path.asList()), builder);
    }

    @Override
    public JsonPath parse(final StringReader reader) throws CommandSyntaxException {
        return JsonPath.parse(reader);
    }
}