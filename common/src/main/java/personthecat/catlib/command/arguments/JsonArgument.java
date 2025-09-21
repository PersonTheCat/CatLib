package personthecat.catlib.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.command.CommandUtils;
import personthecat.catlib.data.Lazy;
import personthecat.catlib.util.PathUtils;
import xjs.data.JsonObject;
import xjs.data.serialization.JsonContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.json.XjsUtils.readSuppressing;
import static personthecat.catlib.util.PathUtils.extension;

public class JsonArgument implements ArgumentType<JsonArgument.Result> {
    public static final ArgumentTypeInfo<JsonArgument, ?> INFO = new Info();
    private static final DynamicCommandExceptionType UNSUPPORTED_FORMAT =
        new DynamicCommandExceptionType(t -> Component.translatable("catlib.errorText.unsupportedFormat", t));

    private final FileArgument getter;

    public JsonArgument(final Path dir) {
        this(new FileArgument(dir));
    }

    protected JsonArgument(final FileArgument getter) {
        this.getter = getter;
    }

    @Override
    public Result parse(final StringReader reader) throws CommandSyntaxException {
        final Path f = this.getter.parse(reader);
        final String ext = extension(f);
        if (!Files.exists(f) && !(Files.isDirectory(f) || JsonContext.isKnownFormat(f.toFile()))) {
            throw UNSUPPORTED_FORMAT.createWithContext(reader, ext);
        }
        return new Result(this.getter.dir, f);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {
        final Stream<String> neighbors = CommandUtils.getLastArg(ctx, JsonArgument.class, Result.class)
            .map(result -> this.getter.suggestPaths(ctx, result.file))
            .orElseGet(() -> this.getter.suggestPaths(ctx));
        return SharedSuggestionProvider.suggest(neighbors, builder);
    }

    public static class Result {
        private final Path root;
        public final Path file;
        public final Lazy<JsonObject> json;

        private Result(final Path root, final Path file) {
            this.root = root;
            this.file = file;
            this.json = Lazy.of(() -> {
                synchronized(this) {
                    return readSuppressing(file).orElseGet(JsonObject::new);
                }
            });
        }

        public Stream<String> getNeighbors() {
            return PathUtils.getContents(this.root, this.file).map(PathUtils::noExtension);
        }
    }

    private static class Info implements ArgumentTypeInfo<JsonArgument, Info.Template> {

        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buf) {
            buf.writeUtf(template.dir);
        }

        @Override
        public @NotNull Template deserializeFromNetwork(FriendlyByteBuf buf) {
            return new Template(buf.readUtf());
        }

        @Override
        public void serializeToJson(Template template, com.google.gson.JsonObject json) {
            json.addProperty("dir", template.dir);
        }

        @Override
        public @NotNull Template unpack(JsonArgument ja) {
            return new Template(ja.getter.dir.toAbsolutePath().toString());
        }

        private class Template implements ArgumentTypeInfo.Template<JsonArgument> {
            private final String dir;

            private Template(String dir) {
                this.dir = dir;
            }

            @Override
            public @NotNull JsonArgument instantiate(CommandBuildContext ctx) {
                return new JsonArgument(Path.of(this.dir));
            }

            @Override
            public @NotNull ArgumentTypeInfo<JsonArgument, ?> type() {
                return Info.this;
            }
        }
    }
}