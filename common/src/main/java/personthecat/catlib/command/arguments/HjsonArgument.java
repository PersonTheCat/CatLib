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
import org.hjson.JsonObject;
import personthecat.catlib.command.CommandUtils;
import personthecat.catlib.data.JsonType;
import personthecat.catlib.data.Lazy;
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.McUtils;
import personthecat.catlib.util.PathUtils;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static personthecat.catlib.exception.Exceptions.cmdSyntax;
import static personthecat.catlib.util.HjsonUtils.readSuppressing;
import static personthecat.catlib.util.PathUtils.extension;

@SuppressWarnings("unused")
public class HjsonArgument implements ArgumentType<HjsonArgument.Result> {

    public static void register() {
        ArgumentTypes.register(LibReference.MOD_ID + ":hjson_argument", HjsonArgument.class,
            new EmptyArgumentSerializer<>(() -> new HjsonArgument(McUtils.getConfigDir())));
    }

    private final FileArgument getter;

    public HjsonArgument(final File dir) {
        this(dir, true);
    }

    public HjsonArgument(final File dir, final boolean recursive) {
        this.getter = new FileArgument(dir, recursive);
    }

    @Override
    public Result parse(final StringReader reader) throws CommandSyntaxException {
        final File f = getter.parse(reader);
        final String ext = extension(f);
        if (f.exists() && !(f.isDirectory() || JsonType.isSupported(ext))) {
            throw cmdSyntax(reader, "Unsupported format");
        }
        return new Result(getter.dir, f);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {
        final Stream<String> neighbors = CommandUtils.getLastArg(ctx, HjsonArgument.class, Result.class)
            .map(result -> this.getter.suggestPaths(ctx, result.file))
            .orElseGet(() -> this.getter.suggestPaths(ctx));
        return SharedSuggestionProvider.suggest(neighbors, builder);
    }

    public static class Result {

        private final File root;
        public final File file;
        public final Lazy<JsonObject> json;

        private Result(final File root, final File file) {
            this.root = root;
            this.file = file;
            this.json = Lazy.of(() -> {
                synchronized(this) {
                    return readSuppressing(file).orElseGet(JsonObject::new);
                }
            });
        }

        public Stream<String> getNeighbors() {
            return PathUtils.getSimpleContents(root, file);
        }
    }
}