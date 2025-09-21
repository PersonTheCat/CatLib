package personthecat.catlib.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * A collection of tools used for interacting with {@link Path file paths}.
 */
public final class PathUtils {

    private PathUtils() {}

    /**
     * Determines whether the given file path has an extension.
     *
     * @param path The path to a file, complete or incomplete.
     * @return Whether an extension was queried.
     */
    public static boolean hasExtension(final String path) {
        return path.endsWith(".") || !extension(path).isEmpty();
    }

    /**
     * Determines the extension of the input file.
     *
     * @param file The file whose name is being formatted.
     * @return The extension of the given file.
     */
    public static String extension(final Path file) {
        return extension(file.getFileName().toString());
    }

    /**
     * Variant of {@link #extension(Path)} which accepts a string.
     *
     * @param s The name of the file or path being operated on.
     * @return The extension of the given file or path.
     */
    public static String extension(final String s) {
        final int index = s.lastIndexOf(".");
        return index < 1 ? "" : s.substring(index + 1);
    }

    /**
     * Gets the name of the file, minus the extension.
     *
     * @param file The file being operated on.
     * @return The regular filename.
     */
    public static String noExtension(final Path file) {
        return noExtension(file.getFileName().toString());
    }

    /**
     * Variant of {@link #noExtension(Path)} which accepts a string.
     *
     * @param s The name of the file or path being operated on.
     * @return The regular filename.
     */
    public static String noExtension(final String s) {
        final int index = s.lastIndexOf(".");
        return index < 0 ? s : s.substring(0, index);
    }

    /**
     * Determines whether the given file is in a specific folder.
     *
     * @param root The root file which is the expected parent directory.
     * @param f The file being tested.
     * @return Whether <code>root</code> contains <code>f</code>.
     */
    public static boolean isIn(final Path root, final Path f) {
        return f.toAbsolutePath().startsWith(root.toAbsolutePath());
    }

    /**
     * Returns a stream of the relative file paths in the given directory.
     *
     * @param root The root directory of the output paths.
     * @param current The current file or directory being examined.
     * @return A stream of the relative file paths contained at this location.
     */
    public static Stream<String> getContents(final Path root, final Path current) {
        final var dir = Files.isDirectory(current) ? current : current.getParent();
        final var builder = Stream.<String>builder();
        try (final var files = Files.list(dir)) {
            files.forEach(p -> builder.add(root.relativize(p).toString().replace("\\", "/")));
            return builder.build();
        } catch (final IOException e) {
            return Stream.empty();
        }
    }
}