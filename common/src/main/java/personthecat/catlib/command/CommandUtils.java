package personthecat.catlib.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import lombok.experimental.UtilityClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import personthecat.catlib.command.arguments.FileArgument;
import personthecat.catlib.command.arguments.HjsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.McTools;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.Optional;

import static personthecat.catlib.command.CommandSuggestions.ANY_INT;
import static personthecat.catlib.command.CommandSuggestions.ANY_DECIMAL;

@UtilityClass
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class CommandUtils {

    /**
     * Gets the most recent argument of the given type without needing to search by name.
     *
     * @param ctx The standard {@link CommandContext} for the current command.
     * @param type The type of argument being queried for.
     * @param t The return type of the argument being queried for.
     * @param <S> The type of object being wrapped by the command context.
     * @param <T> The type of value stored in the command context.
     * @return The requested value, or else {@link Optional#empty}.
     */
    public static <S, T> Optional<T> getLastArg(final CommandContext<S> ctx, final Class<? extends ArgumentType<? extends T>> type, final Class<? extends T> t) {
        for (int i = ctx.getNodes().size() - 1; i >= 0; i--) {
            final ParsedCommandNode<S> node = ctx.getNodes().get(i);
            if (node.getNode() instanceof ArgumentCommandNode) {
                final ArgumentCommandNode<S, ?> argument = (ArgumentCommandNode<S, ?>) node.getNode();
                if (type.isInstance(argument.getType())) {
                    return Optional.of(ctx.getArgument(argument.getName(), t));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Shorthand method for creating a ranged integer argument.
     *
     * @param name The name of the argument node.
     * @param min The minimum allowed value.
     * @param max The maximum allowed value.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, Integer> arg(final String name, final int min, final int max) {
        return Commands.argument(name, IntegerArgumentType.integer(min, max)).suggests(ANY_INT);
    }

    /**
     * Shorthand method for creating a decimal argument
     *
     * @param name The name of the argument node.
     * @param min The minimum allowed value.
     * @param max The maximum allowed value.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, Double> arg(final String name, final double min, final double max) {
        return Commands.argument(name, DoubleArgumentType.doubleArg(min, max)).suggests(ANY_DECIMAL);
    }

    /**
     * Shorthand method for creating a string argument.
     *
     * @param name The name of the argument node.
     * @param suggests The suggestion provider to be used by the output command node.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, String> arg(final String name, final SuggestionProvider<CommandSourceStack> suggests) {
        return Commands.argument(name, StringArgumentType.string()).suggests(suggests);
    }

    /**
     * Variant of {@link #arg(String, SuggestionProvider)} which returns a greedy string.
     *
     * @param name The name of the argument node.
     * @param suggests The suggestion provider to be used by the output command node.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, String> greedyArg(final String name, final SuggestionProvider<CommandSourceStack> suggests) {
        return Commands.argument(name, StringArgumentType.greedyString()).suggests(suggests);
    }

    /**
     * Shorthand method for creating a block argument.
     *
     * @param name The name of the output argument node.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, BlockInput> blkArg(final String name) {
        return Commands.argument(name, BlockStateArgument.block());
    }

    /**
     * Generates a {@link FileArgument} for the given name. The root folder of this
     * argument will be provided by the {@link ModDescriptor} stored in the current
     * {@link CommandRegistrationContext}. If no context is active, the root folder
     * will default to the main conflg folder.
     *
     * @param name The name of the output argument node.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, File> fileArg(final String name) {
        return fileArg(name, getDefaultRoot());
    }

    /**
     * Variant of {@link #fileArg(String)} which provides an explicit {@link ModDescriptor}.
     *
     * @param name The name of the output argument node.
     * @param mod The descriptor providing the root config folder for the current mod.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, File> fileArg(final String name, final ModDescriptor mod) {
        return fileArg(name, mod.getConfigFolder());
    }

    /**
     * Variant of {@link #filArg(String)} which directly supplies a root folder.
     *
     * @param name The name of the output argument node.
     * @param root The root folder to be used by the file argument parser.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, File> fileArg(final String name, final File root) {
        return Commands.argument(name, new FileArgument(root));
    }

    /**
     * Generates a {@link HjsonArgument} when provided a name. As with {@link #fileArg(String)},
     * the root folder of this argument will be supplied by the active {@link ModDescriptor}.
     *
     * @param name The name of the output argument node.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, HjsonArgument.Result> jsonFileArg(final String name) {
        return jsonFileArg(name, getDefaultRoot());
    }

    /**
     * Variant of {@link #jsonFileArg(String)} which provides an explicit {@link ModDescriptor}.
     *
     * @param name The name of the output argument node.
     * @param mod The descriptor providing the root config folder for the current mod.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, HjsonArgument.Result> jsonFileArg(final String name, final ModDescriptor mod) {
        return jsonFileArg(name, mod.getConfigFolder());
    }

    /**
     * Variant of {@link #jsonFileArg(String)} which directly supplies the root folder.
     *
     * @param name The name of the output argument node.
     * @param root The root folder to be used by the file argument parser.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, HjsonArgument.Result> jsonFileArg(final String name, final File root) {
        return Commands.argument(name, new HjsonArgument(root));
    }

    /**
     * Shorthand method for creating an Hjson path argument.
     *
     * @param name The name of the output argument node.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, PathArgument.Result> jsonPathArg(final String name) {
        return Commands.argument(name, new PathArgument());
    }

    private static File getDefaultRoot() {
        final ModDescriptor activeMod = CommandRegistrationContext.getActiveMod();
        return activeMod != null ? activeMod.getConfigFolder() : McTools.getConfigDir();
    }
}
