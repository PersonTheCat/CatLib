package personthecat.catlib.command.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.command.CommandUtils;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.util.PathUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static personthecat.catlib.util.PathUtils.extension;
import static personthecat.catlib.util.PathUtils.noExtension;

/**
 * Generates references to files on the fly on the command line.
 */
public class FileArgument implements ArgumentType<Path> {
    public static final ArgumentTypeInfo<FileArgument, ?> INFO = new Info();

    public final Path dir;

    public FileArgument(final Path dir) {
        if (!FileIO.mkdirs(dir)) {
            throw new IllegalStateException("Creating directory: " + dir.toAbsolutePath());
        } else if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("FileArgument must be a directory: " + dir.toAbsolutePath());
        }
        this.dir = dir;
    }

    @Override
    public Path parse(final StringReader reader) throws CommandSyntaxException {
        final String path;
        if (reader.peek() == '"') {
            path = reader.readQuotedString();
        } else {
            final int start = reader.getCursor();
            while (reader.canRead() && inPath(reader.peek())) {
                reader.skip();
            }
            path = reader.getString().substring(start, reader.getCursor());
        }
        return this.lazyFile(path.replace("\\_", " "));
    }

    public <S> Stream<String> suggestPaths(final CommandContext<S> ctx) {
        return this.suggestPaths(ctx, this.dir);
    }

    public <S> Stream<String> suggestPaths(final CommandContext<S> ctx, final Path f) {
        final String input = ctx.getInput();
        final int space = input.lastIndexOf(" ");
        final String path = space > 0 ? input.substring(space) : input;
        final boolean simple = !PathUtils.hasExtension(path);

        final Stream<String> paths = PathUtils.getContents(this.dir, f)
            .map(s -> simple ? PathUtils.noExtension(s) : s);
        if (path.startsWith("\"")) {
            return paths.map(s -> "\"" + s + "\"");
        }
        return paths.map(s -> s.replace(" ", "\\_"));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {
        final Stream<String> neighbors = CommandUtils.getLastArg(ctx, FileArgument.class, Path.class)
            .map(file -> this.suggestPaths(ctx, file))
            .orElseGet(() -> this.suggestPaths(ctx));
        return SharedSuggestionProvider.suggest(neighbors, builder);
    }

    private static boolean inPath(final char c) {
        return c == '/' || c == '\\' || c == '(' || c == ')' || StringReader.isAllowedInUnquotedString(c);
    }

    /** Retrieves files without needing extensions. */
    private Path lazyFile(final String path) {
        final var test = this.dir.resolve(path);
        // Prefer files over folders unless extension is provided
        if (!extension(test).isEmpty() && Files.exists(test)) {
            return test;
        }
        return FileIO.locate(test.getParent(), f -> pathMatches(f, test.getFileName().toString()))
            .orElseGet(() -> PathUtils.hasExtension(path) ? test : test.resolveSibling(path + ".djs"));
    }

    private static boolean pathMatches(final Path path, final String expected) {
        final String filename = path.getFileName().getFileName().toString();
        return filename.equals(expected) || noExtension(filename).equals(expected);
    }

    private static class Info implements ArgumentTypeInfo<FileArgument, Info.Template> {

        @Override // todo: this leaks user directories to the server, fix by making file arg relative
        public void serializeToNetwork(Template template, FriendlyByteBuf buf) {
            buf.writeUtf(template.dir);
        }

        @Override
        public @NotNull Template deserializeFromNetwork(FriendlyByteBuf buf) {
            return new Template(buf.readUtf());
        }

        @Override
        public void serializeToJson(Template template, JsonObject json) {
            json.addProperty("dir", template.dir);
        }

        @Override
        public @NotNull Template unpack(FileArgument fa) {
            return new Template(fa.dir.toAbsolutePath().toString());
        }

        private class Template implements ArgumentTypeInfo.Template<FileArgument> {
            private final String dir;

            private Template(String dir) {
                this.dir = dir;
            }

            @Override
            public @NotNull FileArgument instantiate(CommandBuildContext ctx) {
                return new FileArgument(Path.of(this.dir));
            }

            @Override
            public @NotNull ArgumentTypeInfo<FileArgument, ?> type() {
                return Info.this;
            }
        }
    }
}