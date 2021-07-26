package personthecat.catlib.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import org.apache.commons.lang3.CharUtils;
import org.hjson.JsonObject;
import personthecat.catlib.command.CommandUtils;
import personthecat.catlib.util.HjsonUtils;
import personthecat.catlib.util.LibReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static personthecat.catlib.exception.Exceptions.cmdSyntax;

@SuppressWarnings("unused")
public class PathArgument implements ArgumentType<PathArgument.Result> {

    public static void register() {
        ArgumentTypes.register(LibReference.MOD_ID + ":path_argument", PathArgument.class,
            new EmptyArgumentSerializer<>(PathArgument::new));
    }

    public static String serialize(final List<Either<String, Integer>> path) {
        final StringBuilder sb = new StringBuilder();
        for (final Either<String, Integer> either : path) {
            either.ifLeft(s -> {
                sb.append('.');
                sb.append(s);
            });
            either.ifRight(i -> {
                sb.append('[');
                sb.append(i);
                sb.append(']');
            });
        }
        final String s = sb.toString();
        return s.startsWith(".") ? s.substring(1) : s;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {
        final Optional<JsonObject> json = CommandUtils.getLastArg(ctx, HjsonArgument.class, HjsonArgument.Result.class)
            .map(arg -> arg.json.get());
        if (!json.isPresent()) {
            return Suggestions.empty();
        }
        final PathArgument.Result path = CommandUtils.getLastArg(ctx, PathArgument.class, Result.class)
            .orElseGet(() -> new PathArgument.Result(Collections.emptyList()));
        return SharedSuggestionProvider.suggest(HjsonUtils.getPaths(json.get(), path), builder);
    }

    @Override
    public Result parse(final StringReader reader) throws CommandSyntaxException {
        final List<Either<String, Integer>> path = new ArrayList<>();
        final int begin = reader.getCursor();

        while(reader.canRead() && reader.peek() != ' ') {
            final char c = reader.read();
            if (c == '.') {
                checkDot(reader, begin);
            } else if (CharUtils.isAsciiAlphanumeric(c)) {
                path.add(Either.left(c + readKey(reader)));
            } else if (c == '[') {
                checkDot(reader, begin);
                path.add(Either.right(reader.readInt()));
                reader.expect(']');
            } else {
                throw cmdSyntax(reader,"Invalid character");
            }
        }
        return new Result(path);
    }

    private static String readKey(final StringReader reader) {
        final int start = reader.getCursor();
        while (reader.canRead() && inKey(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    private static boolean inKey(final char c) {
        return c != '.' && CharUtils.isAsciiAlphanumeric(c);
    }

    private static void checkDot(final StringReader reader, final int begin) throws CommandSyntaxException {
        final int cursor = reader.getCursor();
        final char last = reader.getString().charAt(cursor - 2);
        if (cursor - 1 == begin || last == '.') {
            throw cmdSyntax(reader,"Unexpected accessor");
        }
    }

    /** Provides a concrete wrapper for path arguments. */
    public static class Result {

        public final List<Either<String, Integer>> path;

        public Result(final List<Either<String, Integer>> path) {
            this.path = path;
        }
    }
}