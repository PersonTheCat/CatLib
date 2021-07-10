package personthecat.catlib.io;

import personthecat.catlib.exception.Ex;
import personthecat.catlib.exception.ResourceException;
import personthecat.catlib.util.Sh;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import personthecat.fresult.Result;
import personthecat.fresult.Void;
import personthecat.fresult.functions.ThrowingRunnable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Log4j2
@UtilityClass
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
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
     * Copies a file into the given directory with no explicit options.
     *
     * @param f The file being copied.
     * @param dir The directory being copied into.
     * @return The result of this operation, which you may wish to rethrow.
     */
    public static Result<Path, IOException> copy(final File f, final File dir) {
        return Result.of(() -> Files.copy(f.toPath(), new File(dir, f.getName()).toPath()))
            .ifErr(e -> log.error("Copying file", e));
    }

    /**
     * Returns a list of all files in the given directory. Unlike {@link File#listFiles}, this
     * method does not return <code>null</code>.
     *
     * @param dir The directory being operated on.
     * @return An array of all files in the given directory.
     */
    @Nonnull
    @CheckReturnValue
    public static File[] listFiles(final File dir) {
        return Sh.nullable(dir.listFiles()).orElseGet(() -> new File[0]);
    }

    /**
     * Returns a list of all files in the given directory when applying the given filter. Unlike
     * {@link File#listFiles(FileFilter)}, this method never returns <code>null</code>.
     *
     * @param dir The directory being operated on.
     * @param filter A predicate determining whether a given file should be included.
     * @return An array containing the requested files.
     */
    @Nonnull
    @CheckReturnValue
    public static File[] safeListFiles(final File dir, final FileFilter filter) {
        return Optional.ofNullable(dir.listFiles(filter)).orElse(new File[0]);
    }

    /**
     * Recursive variant of {@link #listFiles(File)}. Includes the contents of each file in every
     * subdirectory for the given folder.
     *
     * @param dir The directory being operated on.
     * @return A {@link List} of <em>all</em> files in the given directory.
     */
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
     * @param dir The directory being operated on.
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
        return readLines(f).map(l -> String.join("\n", l));
    }

    /**
     * Moves a file to the given backup directory.
     *
     * @param dir The directory where this backup will be stored.
     * @param f The file being backed up.
     * @return The number of backups of this file that now exist.
     */
    public static int backup(final File dir, final File f) {
        if (!mkdirs(dir)) {
            throw Ex.resourceF("Error creating backup directory: {}", dir);
        }
        final File backup = new File(dir, f.getName());
        final BackupHelper helper = new BackupHelper(f);
        final int count = helper.cycle(dir);
        if (fileExists(backup)) {
            throw Ex.resourceF("Could not rename backups: {}", f.getName());
        }
        if (!f.renameTo(backup)) {
            throw Ex.resourceF("Error moving {} to backups", f.getName());
        }
        return count + 1;
    }

    /**
     * Renames a file when given a top-level name only.
     *
     * @param f The file being renamed.
     * @param name The new name for this file.
     */
    public static void rename(final File f, final String name) {
        final File path = new File(f.getParentFile(), name);
        if (!f.renameTo(path)) {
            throw Ex.runtimeF("Cannot rename: {}", path);
        }
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
    @SuppressWarnings("RedundantCast")
    public static Result<Void, IOException> copyStream(final InputStream is, final OutputStream os) {
        return Result.of((ThrowingRunnable<IOException>) () -> {
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
            .of(Result.wrapVoid(os -> copyStream(is, os).throwIfErr()))
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
    public static Optional<InputStream> getResource(final String path) {
        final String file = path.startsWith("/") ? path : "/" + path;
        return Sh.nullable(FileIO.class.getResourceAsStream(path));
    }

    /**
     * Retrieves an asset from the jar file or resources directory.
     *
     * @throws ResourceException If the file is not found.
     * @param path The path to the file, with or without the beginning <code>/</code>.
     * @return The resource as an {@link InputStream}.
     */
    public static InputStream getRequiredResource(final String path) {
        return getResource(path).orElseThrow(() ->
            Ex.resourceF("The required file \"{}\" does not exist.", path));
    }

    /**
     * Returns a resource from the jar as a string.
     *
     * @param path The path to the resource being read.
     * @return The resource as a string, or else {@link Optional#empty}.
     */
    public static Optional<String> getResourceAsString(final String path) {
        return getResource(path).flatMap(FileIO::readString);
    }

    /**
     * Parses an input stream as a regular string.
     *
     * @param is The stream being copied out of.
     * @return The file as a string, or else {@link Optional#empty}, if err.
     */
    private static Optional<String> readString(final InputStream is) {
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        final StringBuilder sb = new StringBuilder();

        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(NEW_LINE);
            }
            return Sh.full(sb.toString());
        } catch (IOException e) {
            log.error("Error reading input.");
            return Sh.empty();
        } finally {
            try {
                is.close();
                br.close();
            } catch (IOException e) {
                log.error("Error closing streams", e);
            }
        }
    }
}
