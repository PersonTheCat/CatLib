package personthecat.catlib.command.arguments;

import com.google.gson.JsonPrimitive;
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
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.command.CommandUtils;
import personthecat.catlib.data.Lazy;
import personthecat.catlib.util.PathUtils;
import xjs.data.JsonObject;
import xjs.data.serialization.JsonContext;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static personthecat.catlib.serialization.json.XjsUtils.readSuppressing;
import static personthecat.catlib.util.PathUtils.extension;

public class JsonArgument implements ArgumentType<JsonArgument.Result> {
    public static final ArgumentTypeInfo<JsonArgument, Info.Template> INFO = new Info();
    private static final DynamicCommandExceptionType UNSUPPORTED_FORMAT =
        new DynamicCommandExceptionType(t -> Component.translatable("catlib.errorText.unsupportedFormat", t));

    private final FileArgument getter;

    public JsonArgument(final File dir) {
        this(new FileArgument(dir));
    }

    public JsonArgument(final File dir, final boolean recursive) {
        this(new FileArgument(dir, recursive));
    }

    public JsonArgument(final File dir, @Nullable final File preferred, final boolean recursive) {
        this(new FileArgument(dir, preferred, recursive));
    }

    protected JsonArgument(final FileArgument getter) {
        this.getter = getter;
    }

    @Override
    public Result parse(final StringReader reader) throws CommandSyntaxException {
        final File f = this.getter.parse(reader);
        final String ext = extension(f);
        if (f.exists() && !(f.isDirectory() || JsonContext.isKnownFormat(f))) {
            throw UNSUPPORTED_FORMAT.createWithContext(reader, ext);
        }
        return new Result(getter.dir, f);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {
        final Stream<String> neighbors = CommandUtils.getLastArg(ctx, JsonArgument.class, Result.class)
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

    private static class Info implements ArgumentTypeInfo<JsonArgument, Info.Template> {

        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buf) {
            buf.writeUtf(template.dir);
            if (template.preferred != null) {
                buf.writeBoolean(true);
                buf.writeUtf(template.preferred);
            } else {
                buf.writeBoolean(false);
            }
            buf.writeBoolean(template.recursive);
        }

        @Override
        public @NotNull Template deserializeFromNetwork(FriendlyByteBuf buf) {
            return new Template(
                buf.readUtf(),
                buf.readBoolean() ? buf.readUtf() : null,
                buf.readBoolean());
        }

        @Override
        public void serializeToJson(Template template, com.google.gson.JsonObject json) {
            json.addProperty("dir", template.dir);
            json.addProperty("preferred", template.preferred);
            json.add("recursive", new JsonPrimitive(template.recursive));
        }

        @Override
        public @NotNull Template unpack(JsonArgument ja) {
            return new Template(
                ja.getter.dir.getAbsolutePath(),
                ja.getter.preferred != null ? ja.getter.preferred.getAbsolutePath() : null,
                ja.getter.recursive);
        }

        private class Template implements ArgumentTypeInfo.Template<JsonArgument> {
            private final String dir;
            private final @Nullable String preferred;
            private final boolean recursive;

            private Template(String dir, @Nullable String preferred, boolean recursive) {
                this.dir = dir;
                this.preferred = preferred;
                this.recursive = recursive;
            }

            @Override
            public @NotNull JsonArgument instantiate(CommandBuildContext ctx) {
                return new JsonArgument(
                    new File(this.dir), this.preferred != null ? new File(this.preferred) : null, this.recursive);
            }

            @Override
            public @NotNull ArgumentTypeInfo<JsonArgument, ?> type() {
                return Info.this;
            }
        }
    }
}