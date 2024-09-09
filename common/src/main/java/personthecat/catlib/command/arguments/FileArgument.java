package personthecat.catlib.command.arguments;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.command.CommandUtils;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.util.PathUtils;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static personthecat.catlib.io.FileIO.fileExists;
import static personthecat.catlib.io.FileIO.listFiles;
import static personthecat.catlib.util.PathUtils.extension;
import static personthecat.catlib.util.PathUtils.noExtension;

/**
 * Generates references to files on the fly on the command line.
 */
public class FileArgument implements ArgumentType<File> {
    public static final ArgumentTypeInfo<FileArgument, Info.Template> INFO = new Info();

    public final File dir;
    @Nullable public final File preferred;
    public final boolean recursive;

    public FileArgument(final File dir) {
        this(dir, null, true);
    }

    public FileArgument(final File dir, final File preferred) {
        this(dir, preferred, true);
    }

    public FileArgument(final File dir, final boolean recursive) {
        this(dir, null, recursive);
    }

    public FileArgument(final File dir, @Nullable final File preferred, final boolean recursive) {
        if (!(dir.exists() || dir.mkdirs())) {
            throw new IllegalStateException("Creating directory: " + dir.getAbsolutePath());
        } else if (!dir.isDirectory()) {
            throw new IllegalArgumentException("FileArgument must be a directory: " + dir.getAbsolutePath());
        }
        this.dir = dir;
        this.preferred = preferred;
        this.recursive = recursive;
    }

    @Override
    public File parse(final StringReader reader) throws CommandSyntaxException {
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
        // Todo: recursive search is expensive. Maybe return a FileResult.
        return this.lazyFile(path.replace("\\_", " "));
    }

    public <S> Stream<String> suggestPaths(final CommandContext<S> ctx) {
        return this.suggestPaths(ctx, this.dir);
    }

    public <S> Stream<String> suggestPaths(final CommandContext<S> ctx, final File f) {
        final String input = ctx.getInput();
        final int space = input.lastIndexOf(" ");
        final String path = space > 0 ? input.substring(space) : input;
        final boolean simple = !PathUtils.hasExtension(path);

        final Stream<String> paths = PathUtils.getContents(this.dir, f, simple);
        if (path.startsWith("\"")) {
            return paths.map(s -> "\"" + s + "\"");
        }
        return paths.map(s -> s.replace(" ", "\\_"));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> ctx, final SuggestionsBuilder builder) {
        final Stream<String> neighbors = CommandUtils.getLastArg(ctx, FileArgument.class, File.class)
            .map(file -> this.suggestPaths(ctx, file))
            .orElseGet(() -> this.suggestPaths(ctx));
        return SharedSuggestionProvider.suggest(neighbors, builder);
    }

    private static boolean inPath(final char c) {
        return c == '/' || c == '\\' || c == '(' || c == ')' || StringReader.isAllowedInUnquotedString(c);
    }

    /** Retrieves files without needing extensions. */
    private File lazyFile(final String path) {
        final File test = new File(this.dir, path);
        // Prefer files over folders unless extension is provided
        if (!extension(test).isEmpty() && fileExists(test)) {
            return test;
        }
        for (final File f : listFiles(test.getParentFile())) {
            if (test.getName().equals(noExtension(f))) {
                return f;
            }
        }
        if (this.recursive && !path.contains("/")) {
            return FileIO.locateFileRecursive(this.dir, this.preferred,
                f -> test.getName().equals(noExtension(f))).orElse(test);
        }
        return test;
    }

    private static class Info implements ArgumentTypeInfo<FileArgument, Info.Template> {

        @Override // todo: this leaks user directories to the server, fix by making file arg relative
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
        public void serializeToJson(Template template, JsonObject json) {
            json.addProperty("dir", template.dir);
            json.addProperty("preferred", template.preferred);
            json.add("recursive", new JsonPrimitive(template.recursive));
        }

        @Override
        public @NotNull Template unpack(FileArgument fa) {
            return new Template(
                fa.dir.getAbsolutePath(),
                fa.preferred != null ? fa.preferred.getAbsolutePath() : null,
                fa.recursive);
        }

        private class Template implements ArgumentTypeInfo.Template<FileArgument> {
            private final String dir;
            private final @Nullable String preferred;
            private final boolean recursive;

            private Template(String dir, @Nullable String preferred, boolean recursive) {
                this.dir = dir;
                this.preferred = preferred;
                this.recursive = recursive;
            }

            @Override
            public @NotNull FileArgument instantiate(CommandBuildContext ctx) {
                return new FileArgument(
                    new File(this.dir), this.preferred != null ? new File(this.preferred) : null, this.recursive);
            }

            @Override
            public @NotNull ArgumentTypeInfo<FileArgument, ?> type() {
                return Info.this;
            }
        }
    }
}