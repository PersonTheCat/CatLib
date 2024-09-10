package personthecat.catlib.command.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.command.LibSuggestions;
import personthecat.catlib.data.ModDescriptor;

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
     * Provides a {@link FileArgument} with the current mod's config folder as the root.
     */
    public static class RecursiveFile implements ArgumentSupplier<java.io.File> {
        public ArgumentDescriptor<java.io.File> get() {
            final ModDescriptor mod = getActiveModOrThrow();
            return new ArgumentDescriptor<>(new FileArgument(mod.configFolder(), mod.preferredDirectory()));
        }
    }

    /**
     * Provides a {@link FileArgument} which does not search recursively.
     */
    public static class File implements ArgumentSupplier<java.io.File> {
        public ArgumentDescriptor<java.io.File> get() {
            final ModDescriptor mod = getActiveModOrThrow();
            return new ArgumentDescriptor<>(new FileArgument(mod.configFolder(), false));
        }
    }

    /**
     * Provides an {@link JsonArgument} with the current mod's config folder as the root.
     */
    public static class RecursiveJsonFile implements ArgumentSupplier<JsonArgument.Result> {
        public ArgumentDescriptor<JsonArgument.Result> get() {
            final ModDescriptor mod = getActiveModOrThrow();
            return new ArgumentDescriptor<>(new JsonArgument(mod.configFolder(), mod.preferredDirectory(), true));
        }
    }

    /**
     * Provides an {@link JsonArgument} which does not search recursively.
     */
    public static class JsonFile implements ArgumentSupplier<JsonArgument.Result> {
        public ArgumentDescriptor<JsonArgument.Result> get() {
            final ModDescriptor mod = getActiveModOrThrow();
            return new ArgumentDescriptor<>(new JsonArgument(mod.configFolder(), false));
        }
    }

    /**
     * Provides a generic string argument suggesting the text <code>[&lt;any_value&gt;]</code>
     */
    public static class AnyValue implements ArgumentSupplier<String> {
        public ArgumentDescriptor<String> get() {
            return new ArgumentDescriptor<>(StringArgumentType.greedyString(), LibSuggestions.ANY_VALUE);
        }
    }

    /**
     * Provides a generic integer argument suggesting a few random integer values.
     */
    public static class AnyInt implements ArgumentSupplier<Integer> {
        public ArgumentDescriptor<Integer> get() {
            return new ArgumentDescriptor<>(IntegerArgumentType.integer(), LibSuggestions.ANY_INT);
        }
    }

    /**
     * Provides a generic decimal argument suggesting a few random decimal values.
     */
    public static class AnyDouble implements ArgumentSupplier<Double> {
        public ArgumentDescriptor<Double> get() {
            return new ArgumentDescriptor<>(DoubleArgumentType.doubleArg(), LibSuggestions.ANY_DECIMAL);
        }
    }
}
