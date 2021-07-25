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
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.McTools;
import personthecat.catlib.util.PathTools;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static personthecat.catlib.io.FileIO.fileExists;
import static personthecat.catlib.io.FileIO.listFiles;
import static personthecat.catlib.util.PathTools.extension;
import static personthecat.catlib.util.PathTools.noExtension;

/**
 * Generates references to files on the fly on the command line.
 */
public class FileArgument implements ArgumentType<File> {

    public static void register() {
        ArgumentTypes.register(LibReference.MOD_ID + ":file_argument", FileArgument.class,
            new EmptyArgumentSerializer<>(() -> new FileArgument(McTools.getConfigDir())));
    }

    public final File dir;

    public FileArgument(final File dir) {
        if (!(dir.exists() || dir.mkdirs())) {
            throw new IllegalStateException("Creating directory: " + dir.getAbsolutePath());
        } else if (!dir.isDirectory()) {
            throw new IllegalArgumentException("FileArgument must be a directory: " + dir.getAbsolutePath());
        }
        this.dir = dir;
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
        return lazyFile(this.dir, path.replace("\\_", " "));
    }

    public <S> Stream<String> suggestPaths(final CommandContext<S> ctx) {
        return this.suggestPaths(ctx, this.dir);
    }

    public <S> Stream<String> suggestPaths(final CommandContext<S> ctx, final File f) {
        final String input = ctx.getInput();
        final int space = input.lastIndexOf(" ");
        final String path = space > 0 ? input.substring(space) : input;
        final boolean simple = !PathTools.hasExtension(path);

        final Stream<String> paths = PathTools.getContents(this.dir, f, simple);
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
    private static File lazyFile(final File dir, final String path) {
        final File test = new File(dir, path);
        // Prefer files over folders unless extension is provided
        if (!extension(test).isEmpty() && fileExists(test)) {
            return test;
        }
        for (final File f : listFiles(test.getParentFile())) {
            if (test.getName().equals(noExtension(f))) {
                return f;
            }
        }
        return test;
    }
}