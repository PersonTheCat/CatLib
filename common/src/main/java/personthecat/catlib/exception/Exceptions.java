package personthecat.catlib.exception;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.extern.log4j.Log4j2;
import personthecat.catlib.util.LibUtil;

import java.io.File;

import static personthecat.catlib.util.LibUtil.f;

@Log4j2
@SuppressWarnings("unused")
public final class Exceptions {

    private Exceptions() {}

    /**
     * Returns a clean-looking, general-purpose {@link RuntimeException}.
     *
     * @param x The error message to display.
     * @return A new {@link RuntimeException}.
     */
    public static RuntimeException runEx(final String x) {
        return new RuntimeException(x);
    }

    /**
     * Converts any standard exception into a {@link RuntimeException}.
     *
     * @param e The exception being wrapped.
     * @return A new {@link RuntimeException}.
     */
    public static RuntimeException runEx(final Throwable e) {
        return new RuntimeException(e);
    }

    /**
     * Shorthand for {@link RuntimeException#RuntimeException(String, Throwable)}.
     *
     * @param x The error message to display.
     * @param e The exception being wrapped.
     * @return A new {@link RuntimeException}.
     */
    public static RuntimeException runEx(final String x, final Throwable e) {
        return new RuntimeException(x, e);
    }

    /**
     * Shorthand for a {@link RuntimeException} using {@link LibUtil#f}.
     *
     * @param x The string template being interpolated.
     * @param args The interpolated arguments replacing <code>{}</code>.
     * @return A new {@link RuntimeException}.
     */
    public static RuntimeException runEx(final String x, final Object... args) {
        return new RuntimeException(f(x, args));
    }

    /**
     * Shorthand for a {@link UnreachableException}.
     *
     * @return A new {@link UnreachableException}.
     */
    public static UnreachableException unreachable() {
        return new UnreachableException();
    }

    /**
     * Shorthand for a regular {@link ResourceException}.
     *
     * @param x The error message to display.
     * @return A new {@link ResourceException}.
     */
    public static ResourceException resourceEx(final String x) {
        return new ResourceException(x);
    }

    /**
     * Shorthand for a {@link ResourceException} using {@link LibUtil#f}.
     *
     * @param x The string template being interpolated.
     * @param args The interpolated arguments replacing <code>{}</code>.
     * @return A new {@link ResourceException}.
     */
    public static ResourceException resourceEx(final String x, final Object... args) {
        return new ResourceException(f(x, args));
    }

    /**
     * Shorthand for a regular {@link ResourceException} with cause.
     *
     * @param x The error message to display.
     * @param cause The caught exception responsible for this error.
     * @return A new {@link ResourceException}.
     */
    public static ResourceException resourceEx(final String x, final Throwable cause) {
        return new ResourceException(x, cause);
    }

    /**
     * Shorthand for a regular {@link ModSetupException}.
     *
     * @param x The error message to display.
     * @return A new {@link ModSetupException}.
     */
    public static ModSetupException modSetup(final String x) {
        return new ModSetupException(x);
    }

    /**
     * Shorthand for a {@link ModSetupException} using {@link LibUtil#f}.
     *
     * @param x The string template being interpolated.
     * @param args The interpolated arguments replacing <code>{}</code>.
     * @return A new {@link ModSetupException}.
     */
    public static ModSetupException modSetup(final String x, final Object... args) {
        return new ModSetupException(f(x, args));
    }

    /**
     * Creates a {@link DirectoryNotCreatedException} for the given file.
     *
     * @param f The file which could not be created.
     * @return A new {@link DirectoryNotCreatedException}.
     */
    public static DirectoryNotCreatedException directoryNotCreated(final File f) {
        return new DirectoryNotCreatedException(f);
    }

    /**
     * Creates a {@link DirectoryNotCreatedException} when given a file <em>and</em> a cause.
     *
     * @param f The file which could not be created.
     * @param cause The original exception thrown when creating the directory.
     * @return A new {@link DirectoryNotCreatedException}.
     */
    public static DirectoryNotCreatedException directoryNotCreated(final File f, final Throwable cause) {
        return new DirectoryNotCreatedException(f, cause);
    }

    /**
     * Shorthand for a regular {@link JsonFormatException}.
     *
     * @param x The error message to display.
     * @return A new {@link JsonFormatException}.
     */
    public static JsonFormatException jsonFormatEx(final String x) {
        return new JsonFormatException(x);
    }

    /**
     * Shorthand for a regular {@link JsonFormatException} using {@link LibUtil#f}.
     *
     * @param x The string template being interpolated.
     * @param args The interpolated arguments replacing <code>{}</code>.
     * @return A new {@link JsonFormatException}.
     */
    public static JsonFormatException jsonFormatEx(final String x, final Object... args) {
        return new JsonFormatException(f(x, args));
    }

    /**
     * Shorthand for a regular {@link JsonMappingException}.
     *
     * @param parent The name of the object being deserialized.
     * @param field The field which failed to map.
     * @return A new {@link JsonMappingException}.
     */
    public static JsonMappingException mappingEx(final String parent, final String field) {
        return new JsonMappingException(parent, field);
    }

    /**
     * Shorthand for a regular {@link MissingElementException}.
     *
     * @param x The error message to display.
     * @return A new {@link MissingElementException}.
     */
    public static MissingElementException missingElement(final String x) {
        return new MissingElementException(x);
    }

    /**
     * Shorthand for a regular {@link MissingElementException} using {@link LibUtil#f}.
     *
     * @param x The string template being interpolated.
     * @param args The interpolated arguments replacing <code>{}</code>.
     * @return A new {@link MissingElementException}.
     */
    public static MissingElementException missingElement(final String x, final Object... args) {
        return new MissingElementException(f(x, args));
    }

    /**
     * Shorthand for a simple {@link CommandSyntaxException}.
     *
     * @param reader The reader being used to parse an argument.
     * @param msg The error message to display.
     * @return A new {@link CommandSyntaxException}.
     */
    public static CommandSyntaxException cmdSyntax(final StringReader reader, final String msg) {
        final int cursor = reader.getCursor();
        final String input = reader.getString().substring(0, cursor);
        final Message m = new LiteralMessage(msg);
        return new CommandSyntaxException(new SimpleCommandExceptionType(m), m, input, cursor);
    }

    /**
     * Shorthand for a regular {@link CommandExecutionException}.
     *
     * @param x The string template being interpolated.
     * @return A new {@link CommandExecutionException}.
     */
    public static CommandExecutionException cmdEx(final String x) {
        return new CommandExecutionException(x);
    }

    /**
     * Shorthand for a regular {@link CommandExecutionException} using {@link LibUtil#f}.
     *
     * @param x The string template being interpolated.
     * @param args The interpolated arguments replacing <code>{}</code>.
     * @return A new {@link CommandExecutionException}.
     */
    public static CommandExecutionException cmdEx(final String x, final Object... args) {
        return new CommandExecutionException(f(x, args));
    }

    /**
     * Shorthand for a regular {@link BiomeNotFoundException}.
     *
     * @param name The name of the biome being researched.
     * @return A new {@link BiomeNotFoundException}.
     */
    public static BiomeNotFoundException noBiomeNamed(final String name) {
        return new BiomeNotFoundException(name);
    }

    /**
     * Shorthand for a regular {@link BiomeTypeNotFoundException}.
     *
     * @param name The name of the biome category being researched.
     * @return A new {@link BiomeTypeNotFoundException}.
     */
    public static BiomeTypeNotFoundException noBiomeTypeNamed(final String name) {
        return new BiomeTypeNotFoundException(name);
    }

    /**
     * Shorthand for a regular {@link BlockNotFoundException}.
     *
     * @param name The name of the block being researched.
     * @return A new {@link BlockNotFoundException}.
     */
    public static BlockNotFoundException noBlockNamed(final String name) {
        return new BlockNotFoundException(name);
    }

    /**
     * Shorthand for a regular {@link ItemNotFoundException}.
     *
     * @param name The name of the item being researched.
     * @return A new {@link ItemNotFoundException}.
     */
    public static ItemNotFoundException noItemNamed(final String name) {
        return new ItemNotFoundException(name);
    }

    /**
     * Shorthand for a regular {@link InvalidEnumConstantException}.
     *
     * @param name The name of the constant being researched.
     * @param clazz The type of enum containing this constant.
     * @return A new {@link InvalidEnumConstantException}.
     */
    public static InvalidEnumConstantException invalidConstant(final String name, final Class<? extends Enum<?>> clazz) {
        return new InvalidEnumConstantException(name, clazz);
    }
}
