package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.stream.Stream;

import static personthecat.catlib.io.FileIO.listFiles;
import static personthecat.catlib.util.Shorthand.f;

/** A collection of tools used for interacting with file paths and {@link ResourceLocation}s. */
@UtilityClass
@SuppressWarnings("unused")
public class PathUtils {

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
     * Shorthand for {@link #namespaceToSub(ResourceLocation)} using a raw path.
     *
     * @param id The raw path being transformed.
     * @return The transformed path.
     */
    public static String namespaceToSub(final String id) {
        return namespaceToSub(fromRawPath(id));
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
     * Determines whether the given file path has an extension.
     *
     * @param path The path to a file, complete or incomplete.
     * @return Whether an extension was queried.
     */
    public static boolean hasExtension(final String path) {
        return path.endsWith(".") || !extension(new File(path)).isEmpty();
    }

    /**
     * Determines the extension of the input file.
     *
     * @param file The file whose name is being formatted.
     * @return The extension of the given file.
     */
    public static String extension(final File file) {
        return extension(file.getName());
    }

    /**
     * Variant of {@link #extension(File)} which accepts a string.
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
     * Determines whether the given file is in a specific folder.
     *
     * @param root The root file which is the expected parent directory.
     * @param f The file being tested.
     * @return Whether <code>root</code> contains <code>f</code>.
     */
    public static boolean isIn(final File root, final File f) {
        return f.getAbsolutePath().startsWith(root.getAbsolutePath());
    }

    /**
     * Gets the relative path of the given file up to a root folder.
     *
     * <p>
     *   For example, when given the following root folder:
     * </p><pre>
     *   /home/user/.minecraft/config
     * </pre><p>
     *   And the following file:
     * </p><pre>
     *   /home/user/.minecraft/config/backups/demo.txt
     * </pre><p>
     *   The following path will be output:
     * </p><pre>
     *   backups/demo.txt
     * </pre>
     *
     * @param root The root directory for the output path.
     * @param f The file which the path is generated from.
     * @return A relative path from <code>root</code> to <code>f</code>.
     */
    public static String getRelativePath(final File root, final File f) {
        if (root.equals(f)) {
            return "/";
        }
        final String rootPath = root.getAbsolutePath();
        final String filePath = f.getAbsolutePath();

        if (rootPath.length() + 1 > filePath.length()) {
            return filePath;
        }
        return filePath.substring(rootPath.length() + 1);
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
     * By default, this method excludes the extensions from the output files.
     *
     * @param root The root directory of the output paths.
     * @param current The current file or directory being examined.
     * @return A stream of the relative file paths contained at this location.
     */
    public static Stream<String> getSimpleContents(final File root, final File current) {
        return getContents(root, current, true);
    }

    /**
     * Variant of {@link #getSimpleContents(File, File)} in which the current
     * directory is also the root directory.
     *
     * @param current The current directory being examined.
     * @return The relative file paths in this directory.
     */
    public static Stream<String> getSimpleContents(final File current) {
        return getContents(current, current, true);
    }

    /**
     * Variant of {@link #getContents(File, File, boolean)} in which the current
     * directory is also the root directory.
     *
     * @param current The current directory being examined.
     * @param simple Whether to exclude extensions.
     * @return The relative file paths in this directory.
     */
    public static Stream<String> getContents(final File current, boolean simple) {
        return getContents(current, current, simple);
    }

    /**
     * Returns a stream of the relative file paths in the given directory.
     *
     * @param root The root directory of the output paths.
     * @param current The current file or directory being examined.
     * @param simple Whether to exclude extensions.
     * @return A stream of the relative file paths contained at this location.
     */
    public static Stream<String> getContents(final File root, final File current, boolean simple) {
        final File dir = current.isDirectory() ? current : current.getParentFile();
        final Stream<File> files = Stream.of(listFiles(dir));
        return simple ? files.map(f -> noExtension(formatContents(root, f)))
            : files.map(f -> formatContents(root, f));
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
        return f.getAbsolutePath()
            .substring(root.getAbsolutePath().length())
            .replace("\\", "/")
            .substring(1);
    }

    /**
     * Converts a {@link ResourceLocation} to a raw PNG texture path.
     *
     * @param id The texture's identifier.
     * @return The raw path pointing to the image.
     */
    public static String asTexturePath(final ResourceLocation id) {
        return asTexturePath(id.getNamespace(), id.getPath());
    }

    /**
     * Generates a raw PNG texture path when given a mod's ID and relative
     * file path.
     *
     * @param mod The mod's ID.
     * @param path The relative path to the image.
     * @return The raw path pointing to the image.
     */
    public static String asTexturePath(final String mod, final String path) {
        return f("/assets/{}/textures/{}.png", mod, path);
    }

    /**
     * Converts a {@link ResourceLocation} to a raw JSON model path.
     *
     * @param id The model's identifier.
     * @return The raw path pointing to the model.
     */
    public static String asModelPath(final ResourceLocation id) {
        return asModelPath(id.getNamespace(), id.getPath());
    }

    /**
     * Generates a raw JSON model path when given a mod's ID and relative
     * file path.
     *
     * @param mod The mod's ID.
     * @param path The relative path to the model.
     * @return The raw path pointing to the model.
     */
    public static String asModelPath(final String mod, final String path) {
        return f("/assets/{}/models/{}.json", mod, path);
    }

    /**
     * Converts a {@link ResourceLocation} to a raw block state model path.
     *
     * @param id The model's identifier.
     * @return The raw path pointing to the model.
     */
    public static String asBlockStatePath(final ResourceLocation id) {
        return asBlockStatePath(id.getNamespace(), id.getPath());
    }

    /**
     * Generates a raw JSON block state path when given a mod's ID and
     * relative file path.
     *
     * @param mod The mod's ID.
     * @param path The relative path to the model.
     * @return The raw path pointing to the model.
     */
    public static String asBlockStatePath(final String mod, final String path) {
        return f("/assets/{}/blockstates/{}.json", mod, path);
    }

    /**
     * Converts a raw file path (starting with <code>assets</code> or
     * <code>data</code>) into a {@link ResourceLocation}. The type of
     * file will be accounted for and elided from the return value.
     *
     * <p>These examples demonstrate the expected output:
     *
     * <ul>
     *   <li>
     *     In: <code>assets/minecraft/textures/block/cobblestone.png</code><br>
     *     Out: <code>minecraft:block/cobblestone</code>
     *   </li>
     *   <li>
     *     In: <code>assets/osv/models/block/overlay.json</code><br>
     *     Out: <code>osv:block/overlay</code>
     *   </li>
     * </ul>
     *
     * <p>In the event where no content root is provided in the path,
     * the input is assumed to already be an identifier and the following
     * behavior can be expected:
     *
     * <ul>
     *   <li>
     *     In: <code>block/cobblestone</code><br>
     *     Out: <code>minecraft:block/cobblestone</code>
     *   </li>
     * </ul>
     *
     * @param path The raw path pointing to a resource or {@link ResourceLocation}
     *             as a string.
     * @return The equivalent {@link ResourceLocation} for this path.
     */
    public static ResourceLocation fromRawPath(final String path) {
        final int content = contentRoot(path);
        if (content < 0) return new ResourceLocation(path);
        final String fromMod = path.substring(content + 1);

        final int mod = fromMod.indexOf("/");
        if (mod < 0) return new ResourceLocation(path);
        final String modName = fromMod.substring(0, mod);
        final String fullPath = fromMod.substring(mod + 1);

        final int key = fullPath.indexOf("/");
        if (key < 0) return new ResourceLocation(modName, fullPath + 1);

        return new ResourceLocation(modName, noExtension(fullPath.substring(key + 1)));
    }

    /**
     * Determines the type of content being pointed at by the given path.
     * If no content type can be determined from this path, <code>null</code>
     * will be returned instead.
     *
     * <p>These examples should demonstrate the expected behavior:
     *
     * <ul>
     *   <li>
     *     In: <code>assets/minecraft/textures/block/oak_door.png</code><br>
     *     Out: <code>textures</code></li>
     *   <li>
     *     In: <code>assets/minecraft/models/block/oak_door.json</code><br>
     *     Out: <code>models</code>
     *   </li>
     * </ul>
     *
     * @param path The raw file path for any resource.
     * @return The type of content, e.g. <code>textures</code>, or else <code>null</code>.
     */
    @Nullable
    public static String contentType(final String path) {
        final int content = contentRoot(path);
        if (content < 0) {
            // Assume this path starts just after the mod.
            final int afterMod = path.indexOf("/");
            if (afterMod < 0) return null; // Only one path element.
            return path.substring(0, afterMod + 1);
        }
        final String fromMod = path.substring(content + 1);

        final int mod = fromMod.indexOf("/");
        if (mod < 0) return null; // Would be "assets/mod"
        final String fullPath = fromMod.substring(mod + 1);

        final int fromType = fullPath.indexOf("/");
        if (fromType < 0) return null; // Would be "assets/mod/file"

        return fullPath.substring(0, fromType);
    }

    private static int contentRoot(final String path) {
        final int assets = path.indexOf("assets/");
        if (assets > 0) {
            return assets + "assets".length();
        }
        final int data = path.indexOf("data/");
        if (data > 0) {
            return data + "data".length();
        }
        return -1;
    }
}