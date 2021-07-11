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
     *
     * For example, the resource ID <code>minecraft:block/id</code> will be
     * converted into <code>block/minecraft/id</code>
     *
     * Note: this is unable to output backslashes instead of forward slashes.
     */
    public static String namespaceToSub(final ResourceLocation id) {
        return id.getPath().replaceFirst("(blocks?|items?|^)[/\\\\]?", "$1/" + id.getNamespace() + "/");
    }

    /** Shorthand for {@link #namespaceToSub(ResourceLocation)} using a regular string. */
    public static String namespaceToSub(final String id) {
        return namespaceToSub(new ResourceLocation(id));
    }

    /** Removes all parent directories from a resource location as a string. */
    public static String endOfPath(final String path) {
        final String[] split = path.split("[/\\\\]");
        return split[split.length - 1];
    }

    public static String endOfPath(final ResourceLocation id) {
        return endOfPath(id.getPath());
    }

    /** Determines the extension of the input `file`. */
    public static String extension(final File file) {
        final String name = file.getName();
        return name.substring(name.lastIndexOf(".") + 1);
    }

    /** Gets the name of the file, minus the extension. */
    public static String noExtension(final File file) {
        return noExtension(file.getName());
    }

    /** Returns the full contents of `s` up to the last dot. */
    public static String noExtension(final String s) {
        final int extIndex = s.lastIndexOf(".");
        if (extIndex < 0) {
            return s;
        }
        return s.substring(0, extIndex);
    }

    /** Returns the end of the input path. */
    public static String filename(final String path) {
        final String[] split = path.split("[/\\\\]");
        return split[split.length - 1];
    }

    /**
     * Prepends a new string of text before the last part of a file path.
     *
     * Note: empty strings would probably produce invalid results.
     */
    public static String prependFilename(final String path, final String prefix) {
        return path.replaceFirst("^(.*[/\\\\])*([^/\\\\]+)$", "$1" + prefix + "$2");
    }

    public static Stream<String> getSimpleContents(File current) {
        return getSimpleContents(current, current);
    }

    public static Stream<String> getSimpleContents(File root, File current) {
        final File dir = current.isDirectory() ? current : current.getParentFile();
        return Stream.of(listFiles(dir))
            .map(f -> formatContents(root, f));
    }

    private static String formatContents(File root, File f) {
        final String edit = f.getAbsolutePath()
            .replace(root.getAbsolutePath(), "")
            .replace("\\", "/")
            .substring(1);
        return noExtension(edit);
    }
}