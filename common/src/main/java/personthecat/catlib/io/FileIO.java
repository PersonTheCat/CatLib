package personthecat.catlib.io;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.exception.DirectoryNotCreatedException;
import personthecat.catlib.exception.ResourceException;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import personthecat.fresult.OptionalResult;
import personthecat.fresult.PartialResult;
import personthecat.fresult.Result;
import personthecat.fresult.Void;
import personthecat.fresult.functions.ThrowingConsumer;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.Optional.empty;
import static personthecat.catlib.exception.Exceptions.directoryNotCreated;
import static personthecat.catlib.exception.Exceptions.runEx;
import static personthecat.catlib.exception.Exceptions.resourceEx;
import static personthecat.catlib.util.Shorthand.full;
import static personthecat.catlib.util.Shorthand.nullable;

@Log4j2
@UtilityClass
@ParametersAreNonnullByDefault
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class FileIO {

    /** For outputting the correct new line type. */
    private static final String NEW_LINE = System.lineSeparator();

    /** The size of the array used for copy operations. */
    private static final int BUFFER_SIZE = 1024;

    /**
     * Creates the input directory file and parent files, as needed. Unlike {@link File#mkdirs},
     * this method never throws a {@link SecurityException} and instead returns <code>false</code>
     * if any error occurs.
     *
     * @param f The directory being created.
     * @return Whether the file exists or was created. False if err.
     */
    @CheckReturnValue
    public static boolean mkdirs(final File f) {
        return Result.suppress(() -> f.exists() || f.mkdirs()).orElse(false);
    }

    /**
     * Variant of {@link #mkdirs(File)} which accepts multiple files. Note that this method does
     * not return whether the file exists or has been created and instead will throw an exception
     * if the operation fails.
     *
     * @param files A series of directories being created.
     * @throws DirectoryNotCreatedException If any error occurs when creating the directory.
     */
    public static void mkdirsOrThrow(final File... files) {
        for (final File f : files) {
            Result.suppress(() -> f.exists() || f.mkdirs())
                .mapErr(e -> directoryNotCreated(f, e))
                .filter(b -> b, () -> directoryNotCreated(f))
                .throwIfErr();
        }
    }

    /**
     * Determines whether the given file currently exists. Unlike {@link File#exists}, this method
     * never throws a {@link SecurityException} and instead returns <code>false</code> if any
     * error occurs.
     *
     * @param f The file being operated on.
     * @return Whether the file exists. False if err.
     */
    @CheckReturnValue
    public static boolean fileExists(final File f) {
        return Result.suppress(f::exists).orElse(false);
    }

    /**
     * Copies a file to the given location with no explicit options. The output is allowed
     * to be specified either as a directory which will contain the new file or as the actual
     * file being written.
     *
     * <p>In the event where the file being copied is a directory, its contents will also be
     * copied and the output directory will <b>always</b> be <code>to</code>.
     *
     * @param f The file being copied.
     * @param to The directory or file being copied into.
     * @return The result of this operation, which you may wish to rethrow.
     */
    public static Result<Path, IOException> copy(final File f, final File to) {
        final PartialResult<Path, IOException> partial;
        if (f.isDirectory()) {
            partial = Result.of(() -> {
                FileUtils.copyDirectory(f, to);
                return to.toPath();
            });
        } else {
            final File output = to.isDirectory() ? new File(to, f.getName()) : to;
            partial = Result.of(() -> Files.copy(f.toPath(), output.toPath()));
        }
        return partial.ifErr(e -> log.error("Copying file", e));
    }

    /**
     * Moves a file to the given location with no explicit options. The output is allowed
     * to be specified either as a directory which will contain the new file or as the actual
     * file being written.
     *
     * @param f The file being moved.
     * @param to The directory or file being moved into.
     * @return The result of this operation, which you may wish to rethrow.
     */
    public static Result<Path, IOException> move(final File f, final File to) {
        final File output = to.isDirectory() ? new File(to, f.getName()) : to;
        return Result.of(() -> Files.move(f.toPath(), output.toPath()))
            .ifErr(e -> log.error("moving file", e));
    }

    /**
     * Returns a list of all files in the given directory. Unlike {@link File#listFiles}, this
     * method does not return <code>null</code>.
     *
     * @param dir The directory being operated on.
     * @return An array of all files in the given directory.
     */
    @NotNull
    @CheckReturnValue
    public static File[] listFiles(final File dir) {
        return nullable(dir.listFiles()).orElseGet(() -> new File[0]);
    }

    /**
     * Returns a list of all files in the given directory when applying the given filter. Unlike
     * {@link File#listFiles(FileFilter)}, this method never returns <code>null</code>.
     *
     * @param dir The directory being operated on.
     * @param filter A predicate determining whether a given file should be included.
     * @return An array containing the requested files.
     */
    @NotNull
    @CheckReturnValue
    public static File[] listFiles(final File dir, final FileFilter filter) {
        return nullable(dir.listFiles(filter)).orElse(new File[0]);
    }

    /**
     * Recursive variant of {@link #listFiles(File)}. Includes the contents of each file in every
     * subdirectory for the given folder.
     *
     * @param dir The directory being operated on.
     * @return A {@link List} of <em>all</em> files in the given directory.
     */
    @NotNull
    @CheckReturnValue
    public static List<File> listFilesRecursive(final File dir) {
        if (dir.isFile()) {
            return Collections.emptyList();
        }
        final List<File> files = new ArrayList<>();
        listFilesInto(files, dir);
        return files;
    }

    /**
     * Recursively stores a reference to each file in the given directory in the provided array.
     *
     * @param files The array storing the final list of files.
     * @param dir   The directory being operated on.
     */
    private static void listFilesInto(final List<File> files, final File dir) {
        final File[] inDir = dir.listFiles();
        if (inDir != null) {
            for (final File f : inDir) {
                if (f.isDirectory()) {
                    listFilesInto(files, f);
                } else {
                    files.add(f);
                }
            }
        }
    }

    /**
     * Variant of {@link #listFilesRecursive(File)} which filters based on a predicate.
     *
     * @param dir    The root directory which may contain the expected files.
     * @param filter A predicate used to match the expected files.
     * @return Every matching file nested within this directory.
     */
    public static List<File> listFilesRecursive(final File dir, final FileFilter filter) {
        if (dir.isFile()) {
            return Collections.emptyList();
        }
        final List<File> files = new ArrayList<>();
        listFilesInto(files, dir, filter);
        return files;
    }

    /**
     * Variant of {@link #listFilesInto(List, File)} which filters based on a predicate.
     *
     * @param files  The array storing the final list of files.
     * @param dir    The directory being operated on.
     * @param filter A filter used for matching files.
     */
    private static void listFilesInto(final List<File> files, final File dir, final FileFilter filter) {
        final File[] inDir = dir.listFiles();
        if (inDir != null) {
            for (final File f : inDir) {
                if (f.isDirectory()) {
                    listFilesInto(files, f);
                } else if (filter.accept(f)) {
                    files.add(f);
                }
            }
        }
    }

    /**
     * Recursively searches through the given directory until the first matching file is found.
     *
     * @param dir    The root directory which may contain the expected file.
     * @param filter A predicate used to match the expected file.
     * @return The requested file, or else {@link Optional#empty}.
     */
    @CheckReturnValue
    public static Optional<File> locateFileRecursive(final File dir, final FileFilter filter) {
        final File[] inDir = dir.listFiles();
        if (inDir != null) {
            for (final File f : inDir) {
                if (f.isDirectory()) {
                    final Optional<File> found = locateFileRecursive(f, filter);
                    if (found.isPresent()) {
                        return found;
                    }
                } else if (filter.accept(f)) {
                    return full(f);
                }
            }
        }
        return empty();
    }

    /**
     * Variant of {@link #locateFileRecursive(File, FileFilter)} which first looks in a
     * preferred directory.
     *
     * @param dir       The root directory which may contain the expected file.
     * @param preferred The first directory to search through.
     * @param filter    A predicate used to match the expected file.
     * @return The requested file, or else {@link Optional#empty}.
     */
    @CheckReturnValue
    public static Optional<File> locateFileRecursive(final File dir, @Nullable final File preferred, final FileFilter filter) {
        if (preferred == null) return locateFileRecursive(dir, filter);
        final Optional<File> inPreferred = locateFileRecursive(preferred, filter);
        return inPreferred.isPresent() ? inPreferred : locateFileRecursiveInternal(dir, preferred, filter);
    }

    /**
     * Internal variant of {@link #locateFileRecursive(File, File, FileFilter)} which has
     * already searched through the preferred directory.
     *
     * @param dir       The root directory which may contain the expected file.
     * @param preferred The first directory to search through.
     * @param filter    A predicate used to match the expected file.
     * @return The requested file, or else {@link Optional#empty}.
     */
    @CheckReturnValue
    private static Optional<File> locateFileRecursiveInternal(final File dir, final File preferred, final FileFilter filter) {
        final File[] inDir = dir.listFiles(f -> !f.equals(preferred));
        if (inDir != null) {
            for (final File f : inDir) {
                if (f.isDirectory()) {
                    final Optional<File> found = locateFileRecursiveInternal(f, preferred, filter);
                    if (found.isPresent()) {
                        return found;
                    }
                } else if (filter.accept(f)) {
                    return full(f);
                }
            }
        }
        return empty();
    }

    /**
     * Attempts to read the contents of a file. Returns an error if any exception occurs.
     *
     * @param f The file being read.
     * @return The contents of the file, or else {@link Result#err}.
     */
    public static Result<String, IOException> readFile(final File f) {
        try {
            return Result.ok(FileUtils.readFileToString(f, Charset.defaultCharset()));
        } catch (final IOException e) {
            return Result.err(e);
        }
    }

    /**
     * Attempts to retrieve the contents of the input file, or else {@link Optional#empty}.
     *
     * @param f The file being read from.
     * @return A list of each line in the given file, or else {@link Optional#empty}.
     */
    @CheckReturnValue
    public static Optional<List<String>> readLines(final File f) {
        return Result.of(() -> Files.readAllLines(f.toPath())).get(Result::IGNORE);
    }

    /**
     * Returns the raw string contents of the given file, or else {@link Optional#empty}.
     *
     * @param f The file being read from.
     * @return A string representation of the given file's contents, or else {@link Optional#empty}.
     */
    @CheckReturnValue
    public Optional<String> contents(final File f) {
        return readFile(f).get();
    }

    /**
     * Copies a file to the given backup directory.
     *
     * @param dir The directory where this backup will be stored.
     * @param f The file being backed up.
     * @return The number of backups of this file that now exist.
     */
    public static int backup(final File dir, final File f) {
        return backup(dir, f, true);
    }

    /**
     * Variant of {@link #backup(File, File, boolean)} which never throws an exception.
     *
     * @param dir The directory where this backup will be stored.
     * @param f The file being backed up.
     * @param copy Whether to additionally copy the file instead of just moving it.
     * @return A result indicating either the number of backups or the error.
     */
    public static Result<Integer, ResourceException> tryBackup(final File dir, final File f, final boolean copy) {
        try {
            return Result.ok(backup(dir, f, copy));
        } catch (final ResourceException e) {
            return Result.err(e);
        }
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
    public static int backup(final File dir, final File f, final boolean copy) {
        if (f.getAbsolutePath().startsWith(dir.getAbsolutePath())) {
            throw resourceEx("Cannot create backup inside backups directory: {}", f.getName());
        }
        if (!mkdirs(dir)) {
            throw resourceEx("Error creating backup directory: {}", dir);
        }
        final File backup = new File(dir, f.getName());
        final BackupHelper helper = new BackupHelper(f);
        final int count = helper.cycle(dir);
        if (fileExists(backup)) {
            throw resourceEx("Could not rename backups: {}", f.getName());
        }
        if (copy) {
            copy(f, dir).ifErr(Result::THROW);
        } else if (!f.renameTo(backup)) {
            throw resourceEx("Error moving {} to backups", f.getName());
        }
        return count + 1;
    }

    /**
     * Variant of {@link #truncateBackups(File, File, int)} which never throws an exception.
     *
     * @param dir The directory containing the files being truncated.
     * @param f The being moved into the backup folder.
     * @param count The maximum number of matching backups to keep in this directory.
     * @return A result wrapping the error, if applicable.
     */
    public static Result<Void, ResourceException> tryTruncateBackups(final File dir, final File f, final int count) {
        try {
            truncateBackups(dir, f, count);
            return Result.ok();
        } catch (final ResourceException e) {
            return Result.err(e);
        }
    }

    /**
     * Deletes any matching file in the backup folder, keeping <code>count</code> files.
     *
     * @throws ResourceException If <b>any</b> IO exception occurs in the process.
     * @param dir The directory containing the files being truncated.
     * @param f The being moved into the backup folder.
     * @param count The maximum number of matching backups to keep in this directory.
     */
    public static void truncateBackups(final File dir, final File f, final int count) {
        if (!mkdirs(dir)) {
            throw resourceEx("Error creating backup directory: {}", dir);
        }
        new BackupHelper(f).truncate(dir, count);
    }

    /**
     * Deletes the given file. If this file is a directory, its contents will be
     * deleted recursively.
     *
     * @param f The file or directory being deleted.
     */
    public static void delete(final File f) {
        if (f.isFile() && !f.delete()) {
            throw resourceEx("Error deleting file {}.", f.getName());
        } else {
            try {
                Files.walk(f.toPath()).sorted(Comparator.reverseOrder()).forEach(p -> {
                    if (!p.toFile().delete()) {
                        log.error("Error deleting {}", p.toFile().getName());
                    }
                });
            } catch (final IOException e) {
                throw resourceEx("Error deleting directory", e);
            }
        }
    }

    /**
     * Renames a file when given a top-level name only.
     *
     * @param f The file being renamed.
     * @param name The new name for this file.
     */
    public static Result<Void, RuntimeException> rename(final File f, final String name) {
        final File path = new File(f.getParentFile(), name);
        if (!f.renameTo(path)) {
            return Result.err(runEx("Cannot rename: {}", path));
        }
        return Result.ok();
    }

    /**
     * Standard stream copy process. Returns an exception, instead of throwing it.
     * <p>
     *   Note that the given streams <b>will not be closed.</b>.
     * </p>
     * @param is The input stream being copied out of.
     * @param os The output stream being copied into.
     * @return The result of the operation. <b>Any errors should not be ignored</b>.
     */
    public static Result<Void, IOException> copyStream(final InputStream is, final OutputStream os) {
        return Result.of(() -> {
            final byte[] b = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(b)) > 0) {
                os.write(b, 0, length);
            }
        }).ifErr(e -> log.error("Error copying stream", e));
    }

    /**
     * Variant of {@link #copyStream(InputStream, OutputStream)} which accepts a file path
     * parameter instead of an {@link OutputStream} stream directly.
     * <p>
     *   Note that the given input stream <b>will not be closed.</b>.
     * </p>
     * @param is The input stream being copied out of.
     * @return The result of the operation. <b>Any errors should not be ignored</b>.
     */
    @CheckReturnValue
    public static Result<Void, IOException> copyStream(final InputStream is, final String path) {
        return Result.<OutputStream, IOException>with(() -> new FileOutputStream(path))
            .of((ThrowingConsumer<OutputStream, IOException>) os -> copyStream(is, os).throwIfErr())
            .ifErr(e -> log.error("Error creating output stream", e));
    }

    /**
     * Determines whether an asset is present in the jar.
     *
     * @param path The path to the file, with or without the beginning <code>/</code>.
     */
    @CheckReturnValue
    public static boolean resourceExists(final String path) {
        final Optional<InputStream> is = getResource(path);
        is.ifPresent(s -> Result.suppress(s::close).ifErr(e -> log.error("Closing resource", e)));
        return is.isPresent();
    }

    /**
     * Retrieves an asset from the jar file or resources directory.
     *
     * @param path The path to the file, with or without the beginning <code>/</code>.
     */
    @CheckReturnValue
    public static Optional<InputStream> getResource(final String path) {
        final String file = path.startsWith("/") ? path : "/" + path;
        return nullable(FileIO.class.getResourceAsStream(file));
    }

    /**
     * Retrieves an asset from the jar file or resources directory.
     *
     * @throws ResourceException If the file is not found.
     * @param path The path to the file, with or without the beginning <code>/</code>.
     * @return The resource as an {@link InputStream}.
     */
    @CheckReturnValue
    public static InputStream getRequiredResource(final String path) {
        return getResource(path).orElseThrow(() ->
            resourceEx("The required file \"{}\" does not exist.", path));
    }

    /**
     * Returns a resource from the jar as a string.
     *
     * @param path The path to the resource being read.
     * @return The resource as a string, or else {@link Optional#empty}.
     */
    @CheckReturnValue
    public static OptionalResult<String, IOException> getResourceAsString(final String path) {
        return Result.<InputStream, IOException>nullable(getResource(path)).flatMap(FileIO::readString);
    }

    /**
     * Parses an input stream as a regular string.
     *
     * @param is The stream being copied out of.
     * @return The file as a string, or else {@link Optional#empty}, if err.
     */
    @CheckReturnValue
    private static OptionalResult<String, IOException> readString(final InputStream is) {
        return Result.<InputStream, IOException>with(() -> is)
            .and(() -> new BufferedReader(new InputStreamReader(is)))
            .nullable(FileIO::read)
            .ifErr(e -> log.error("Reading string", e));
    }

    /**
     * Parses all lines out of the given reader.
     *
     * @throws IOException Declared by {@link BufferedReader#readLine()}
     * @param reader The buffered reader providing string data.
     * @return A parsed string.
     */
    private static String read(final BufferedReader reader) throws IOException {
        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append(NEW_LINE);
        }
        return sb.toString();
    }
}
