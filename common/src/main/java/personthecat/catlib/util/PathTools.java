package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.stream.Stream;

import static personthecat.catlib.io.FileIO.listFiles;

/** A collection of tools used for interacting with file paths and {@link ResourceLocation}s. */
@UtilityClass
@SuppressWarnings("unused")
public class PathTools {

    /**
     * Converts the input path into a fully-qualified ResourceLocation.
     *
     * @param path The raw file URI being decoded.
     */
    public static ResourceLocation getResourceLocation(final String path) {
        final String namespace = getNamespaceFromPath(path);
        final String result = path
            .replaceAll("(assets|data)[/\\\\]", "")
            .replaceAll(namespace + "[/\\\\]", "")
            .replaceAll("^[/\\\\]", "");
        return new ResourceLocation(namespace, result);
    }

    /**
     * Looks for the first path component after one of the root resource folders.
     *
     * @param path The raw file URI being decoded.
     * @return The namespace of the mod this refers to, if possible, else itself.
     */
    private static String getNamespaceFromPath(final String path) {
        boolean rootFound = false;
        for (String s : path.split("[/\\\\]")) {
            if (rootFound && !s.isEmpty()) {
                return s;
            }
            rootFound |= "assets".equals(s) || "data".equals(s);
        }
        return path;
    }

    /**
     * Reuses a foreign path as a sub path. This function will specifically
     * place the namespace after <code>block/</code> or <code>item/</code>
     * <p>
     *   For example, the resource ID <code>minecraft:block/id</code> will be
     *   converted into <code>block/minecraft/id</code>
     * </p>
     * <p>
     *   Note: this is unable to output backslashes instead of forward slashes.
     * </p>
     *
     * @param id The resource location being transformed.
     * @return The transformed path.
     */
    public static String namespaceToSub(final ResourceLocation id) {
        return id.getPath().replaceFirst("(blocks?|items?|^)[/\\\\]?", "$1/" + id.getNamespace() + "/");
    }

    /**
     * Shorthand for {@link #namespaceToSub(ResourceLocation)} using a regular string.
     *
     * @param id The resource location being transformed.
     * @return The transformed path.
     */
    public static String namespaceToSub(final String id) {
        return namespaceToSub(new ResourceLocation(id));
    }

    /**
     * Removes all parent directories from a resource location as a string.
     * <p>
     *   For example, <code>name:pathA/pathB</code> will be transformed into
     *   <code>pathB</code>.
     * </p>
     * @param path The path being parsed.
     * @return The filename or key at the end of the path.
     */
    public static String filename(final String path) {
        final String[] split = path.split("[/\\\\]");
        return split[split.length - 1];
    }

    /**
     * Variant of {@link #filename(String)} which accepts a {@link ResourceLocation}.
     *
     * @param id The path being parsed.
     * @return The filename or key at the end of the path.
     */
    public static String endOfPath(final ResourceLocation id) {
        return filename(id.getPath());
    }

    /**
     * Determines the extension of the input file.
     */
    public static String extension(final File file) {
        final String name = file.getName();
        final int index = name.lastIndexOf(".");
        return index < 0 ? "" : name.substring(index + 1);
    }

    /**
     * Gets the name of the file, minus the extension.
     *
     * @param file The file being operated on.
     * @return The regular filename.
     */
    public static String noExtension(final File file) {
        return noExtension(file.getName());
    }

    /**
     * Variant of {@link #noExtension(File)} which accepts a string.
     *
     * @param s The name of the file or path being operated on.
     * @return The regular filename.
     */
    public static String noExtension(final String s) {
        final int index = s.lastIndexOf(".");
        return index < 0 ? s : s.substring(0, index);
    }

    /**
     * Prepends a new string of text before the last part of a file path.
     * <p>
     *   For example, prepending <code>bar_</code> onto <code>foo/baz</code>
     *   will output <code>foo/bar_baz</code>
     * </p>
     * <p>
     *   Note: empty strings would probably produce invalid results.
     * </p>
     * @param path The fully-qualified file path.
     * @param prefix The string to prepend to the filename.
     */
    public static String prependFilename(final String path, final String prefix) {
        return path.replaceFirst("^(.*[/\\\\])*([^/\\\\]+)$", "$1" + prefix + "$2");
    }

    /**
     * Returns a stream of the relative file paths in the given directory.
     *
     * @param root The root directory of the output paths.
     * @param current The current file or directory being examined.
     * @return A stream of the relative file paths contained at this location.
     */
    public static Stream<String> getSimpleContents(final File root, final File current) {
        final File dir = current.isDirectory() ? current : current.getParentFile();
        return Stream.of(listFiles(dir)).map(f -> formatContents(root, f));
    }

    /**
     * Variant of {@link #getSimpleContents(File, File)} in which the current
     * directory is also the root directory.
     *
     * @param current The current directory being examined.
     * @return The relative file paths in this directory.
     */
    public static Stream<String> getSimpleContents(final File current) {
        return getSimpleContents(current, current);
    }

    /**
     * Formats the given path to return only a relative path using forward slashes
     * instead of backward slashes.
     *
     * @param root The root directory.
     * @param f The file which the relative path is generated from.
     * @return The formatted path.
     */
    private static String formatContents(final File root, final File f) {
        final String edit = f.getAbsolutePath()
            .substring(root.getAbsolutePath().length())
            .replace("\\", "/")
            .substring(1);
        return noExtension(edit);
    }
}