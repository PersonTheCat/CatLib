package personthecat.catlib.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.io.File;

import static personthecat.catlib.io.FileIO.fileExists;
import static personthecat.catlib.io.FileIO.listFiles;
import static personthecat.catlib.util.PathTools.noExtension;

/**
 * Generates references to files on the fly on the command line.
 */
public class FileArgument implements ArgumentType<File> {

    public final File dir;

    public FileArgument(final File dir) {
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
        return lazyFile(dir, path);
    }

    private static boolean inPath(final char c) {
        return c == '/' || StringReader.isAllowedInUnquotedString(c);
    }

    /** Retrieves files without needing extensions. */
    private static File lazyFile(final File dir, final String path) {
        final File test = new File(dir, path);
        if (fileExists(test)) {
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