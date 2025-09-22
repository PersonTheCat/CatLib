package personthecat.catlib.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.command.LibSuggestions;
import personthecat.catlib.data.ModDescriptor;

import java.nio.file.Path;

import static personthecat.catlib.command.CommandRegistrationContext.getActiveModOrThrow;

/**
 * A few convenience {@link ArgumentSupplier} classes to be used as <code>descriptor</code>s
 * inside of {@link ModCommand}-annotated command builders.
 * <p>
 *   Most of the generic argument types provided by Brigadier and the base game can be used
 *   in command builders by setting a <code>type</code>. Note that this setup is only valid
 *   for {@link ArgumentType}s with no argument constructors.
 * </p>
 */
public final class ArgumentSuppliers {

    private ArgumentSuppliers() {}

    /**
     * Provides a {@link FileArgument} which does not search recursively.
     */
    public static class File implements ArgumentSupplier<Path> {
        public ArgumentDescriptor<Path> get() {
            final ModDescriptor mod = getActiveModOrThrow();
            return new ArgumentDescriptor<>(new FileArgument(mod.configFolder()));
        }
    }

    /**
     * Provides an {@link JsonArgument} which does not search recursively.
     */
    public static class JsonFile implements ArgumentSupplier<JsonArgument.Result> {
        public ArgumentDescriptor<JsonArgument.Result> get() {
            final ModDescriptor mod = getActiveModOrThrow();
            return new ArgumentDescriptor<>(new JsonArgument(mod.configFolder()));
        }
    }
}
