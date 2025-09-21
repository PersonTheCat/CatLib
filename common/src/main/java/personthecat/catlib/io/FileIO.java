package personthecat.catlib.io;

import personthecat.catlib.exception.ResourceException;
import lombok.extern.log4j.Log4j2;
import personthecat.catlib.util.PathUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static personthecat.catlib.util.LibUtil.f;

@Log4j2
@SuppressWarnings("UnusedReturnValue")
public final class FileIO {

    private FileIO() {}

    /**
     * Creates the input directory file and parent files, as needed. Unlike {@link Files#createDirectories},
     * this method never throws a {@link SecurityException} and instead returns <code>false</code>
     * if any error occurs.
     *
     * @param f The directory being created.
     * @return Whether the file exists or was created. False if err.
     */
    public static boolean mkdirs(final Path f) {
        try {
            mkdirsOrThrow(f);
            return true;
        } catch (final Exception ignored) {
            return false;
        }
    }

    /**
     * Variant of {@link #mkdirs(Path)} which accepts multiple files. Note that this method does
     * not return whether the file exists or has been created and instead will throw an exception
     * if the operation fails.
     *
     * @param files A series of directories being created.
     * @throws UncheckedIOException If any error occurs when creating the directory.
     */
    public static void mkdirsOrThrow(final Path... files) {
        for (final var f : files) {
            try {
                Files.createDirectories(f);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Unchecked variant of {@link Files#copy} which may either copy to a directory or the
     * exact file. The caller must ensure that <code>to</code> already exists and is a
     * directory if they wish to copy into this directory. Otherwise, the stream will be
     * copied exactly.
     *
     * <p>In the event where the file being copied is a directory, its contents will also be
     * copied and the output directory will <b>always</b> be <code>to</code>.
     *
     * @param from The file <em>or directory</em> being copied.
     * @param to   The file <em>or directory</em> being copied into.
     */
    public static void copy(final Path from, final Path to) {
        try {
            if (Files.isDirectory(to)) {
                Files.createDirectories(to);
                Files.copy(from, to.resolve(from.getFileName()));
                return;
            }
            try (final Stream<Path> stream = Files.walk(from)) {
                stream.forEach(sourcePath -> {
                    try {
                        final var targetPath = to.resolve(from.relativize(sourcePath));
                        if (Files.isDirectory(sourcePath)) {
                            Files.createDirectories(targetPath); // Create directories if they don't exist
                        } else {
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Moves a file to the given location with no explicit options. The output is allowed
     * to be specified either as a directory which will contain the new file or as the actual
     * file being written.
     *
     * @param f The file being moved.
     * @param to The directory or file being moved into.
     */
    public static void move(final Path f, final Path to) {
        try {
            Files.move(f, Files.isDirectory(to) ? to.resolve(f.getFileName()) : to);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Walks the given directory and cleans up resources for the caller.
     *
     * @param dir     The directory being operated on.
     * @param f       The action to perform on each file, recursively
     */
    public static void forEach(final Path dir, final Consumer<Path> f) {
        forEach(dir, false, f);
    }

    /**
     * Walks the given directory and cleans up resources for the caller.
     *
     * @param dir     The directory being operated on.
     * @param recurse Whether to recurse into subdirectories.
     * @param f       The action to perform on each file, recursively
     */
    public static void forEach(final Path dir, final boolean recurse, final Consumer<Path> f) {
        try (final Stream<Path> paths = recurse ? Files.walk(dir) : Files.list(dir)) {
            paths.forEach(f);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Searches through the given directory until the first matching file is found.
     *
     * @param dir    The root directory which may contain the expected file.
     * @param filter A predicate used to match the expected file.
     * @return The requested file, or else {@link Optional#empty}.
     */
    public static Optional<Path> locate(final Path dir, final Predicate<Path> filter) {
        return locate(dir, false, filter);
    }

    /**
     * Recursively searches through the given directory until the first matching file is found.
     *
     * @param dir     The root directory which may contain the expected file.
     * @param recurse Whether to recurse into subdirectories
     * @param filter  A predicate used to match the expected file.
     * @return The requested file, or else {@link Optional#empty}.
     */
    public static Optional<Path> locate(final Path dir, final boolean recurse, final Predicate<Path> filter) {
        try (final Stream<Path> paths = recurse ? Files.walk(dir) : Files.list(dir)) {
            return paths.filter(filter).findFirst();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Unchecked variant of {@link Files#isSameFile}
     *
     * @param a The first file to compare.
     * @param b The second file to compare.
     * @return <code>true</code> if the paths point to the same file.
     */
    public static boolean isSameFile(final Path a, final Path b) {
        try {
            return Files.isSameFile(a, b);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Copies a file to the given backup directory.
     *
     * @param dir The directory where this backup will be stored.
     * @param f   The file being backed up.
     * @return The number of backups of this file that now exist.
     */
    public static int backup(final Path dir, final Path f) {
        return backup(dir, f, true);
    }

    /**
     * Copies (or moves) a file to the given backup directory.
     *
     * @throws ResourceException If <b>any</b> IO exception occurs.
     * @param dir The directory where this backup will be stored.
     * @param f The file being backed up.
     * @param copy Whether to additionally copy the file instead of just moving it.
     * @return The number of backups of this file that now exist.
     */
    public static int backup(final Path dir, final Path f, final boolean copy) {
        if (PathUtils.isIn(dir, f)) {
            throw new ResourceException(f("Cannot create backup inside backups directory: {}", f.getFileName()));
        }
        if (!mkdirs(dir)) {
            throw new ResourceException(f("Error creating backup directory: {}", dir));
        }
        final Path backup = dir.resolve(f.getFileName());
        final BackupHelper helper = new BackupHelper(f);
        final int count = helper.cycle(dir);
        if (Files.exists(backup)) {
            throw new ResourceException(f("Could not rename backups: {}", f.getFileName()));
        }
        if (copy) {
            copy(f, dir);
        } else {
            move(f, backup);
        }
        return count + 1;
    }

    public static void delete(final Path f) {
        try (final Stream<Path> walk =
                 Files.isRegularFile(f) ? Stream.of(f) : Files.walk(f).sorted(Comparator.reverseOrder())) {
            walk.forEach(p -> {
                try {
                    Files.delete(p);
                } catch (final IOException e) {
                    log.error("Error deleting {}", p.toAbsolutePath(), e);
                }
            });
        } catch (final IOException e) {
            throw new ResourceException("Error deleting " + f.toAbsolutePath(), e);
        }
    }

    /**
     * Renames a file when given a top-level name only.
     *
     * @param f The file being renamed.
     * @param name The new name for this file.
     */
    public static void rename(final Path f, final String name) {
        try {
            Files.move(f, f.resolveSibling(name));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
