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
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.command.arguments.FileArgument;
import personthecat.catlib.command.arguments.JsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.command.arguments.RegistryArgument;
import personthecat.catlib.serialization.json.JsonPath;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.McUtils;

import java.nio.file.Path;
import java.util.Optional;

public final class CommandUtils {

    private CommandUtils() {}

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
    public static <S, T> Optional<T> getLastArg(final CommandContext<S> ctx, final Class<? extends ArgumentType<?>> type, final Class<T> t) {
        for (int i = ctx.getNodes().size() - 1; i >= 0; i--) {
            final ParsedCommandNode<S> node = ctx.getNodes().get(i);
            if (node.getNode() instanceof final ArgumentCommandNode<S, ?> argument) {
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
        return Commands.argument(name, IntegerArgumentType.integer(min, max));
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
        return Commands.argument(name, DoubleArgumentType.doubleArg(min, max));
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
    public static RequiredArgumentBuilder<CommandSourceStack, BlockInput> blkArg(
            final String name, final CommandBuildContext ctx) {
        return Commands.argument(name, BlockStateArgument.block(ctx));
    }

    /**
     * Generates a {@link FileArgument} for the given name. The root folder of this
     * argument will be provided by the {@link ModDescriptor} stored in the current
     * {@link CommandRegistrationContext}. If no context is active, the root folder
     * will default to the main config folder.
     *
     * @param name The name of the output argument node.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, Path> fileArg(final String name) {
        return fileArg(name, getDefaultRoot());
    }

    /**
     * Variant of {@link #fileArg(String)} which provides an explicit {@link ModDescriptor}.
     *
     * @param name The name of the output argument node.
     * @param mod The descriptor providing the root config folder for the current mod.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, Path> fileArg(final String name, final ModDescriptor mod) {
        return fileArg(name, mod.configFolder());
    }

    /**
     * Variant of {@link #fileArg(String)} which directly supplies a root folder.
     *
     * @param name The name of the output argument node.
     * @param root The root folder to be used by the file argument parser.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, Path> fileArg(final String name, final Path root) {
        return Commands.argument(name, new FileArgument(root));
    }

    /**
     * Generates a {@link JsonArgument} when provided a name. As with {@link #fileArg(String)},
     * the root folder of this argument will be supplied by the active {@link ModDescriptor}.
     *
     * @param name The name of the output argument node.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, JsonArgument.Result> jsonFileArg(final String name) {
        return jsonFileArg(name, getDefaultRoot());
    }

    /**
     * Variant of {@link #jsonFileArg(String)} which provides an explicit {@link ModDescriptor}.
     *
     * @param name The name of the output argument node.
     * @param mod The descriptor providing the root config folder for the current mod.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, JsonArgument.Result> jsonFileArg(final String name, final ModDescriptor mod) {
        return jsonFileArg(name, mod.configFolder());
    }

    /**
     * Variant of {@link #jsonFileArg(String)} which directly supplies the root folder.
     *
     * @param name The name of the output argument node.
     * @param root The root folder to be used by the file argument parser.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, JsonArgument.Result> jsonFileArg(final String name, final Path root) {
        return Commands.argument(name, new JsonArgument(root));
    }

    /**
     * Shorthand method for creating a JSON path argument.
     *
     * @param name The name of the output argument node.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, JsonPath> jsonPathArg(final String name) {
        return Commands.argument(name, new PathArgument());
    }

    /**
     * Shorthand for creating a registry argument.
     *
     * @param name The name of the output argument node.
     * @param key  The key of the registry being debugged.
     * @param <T>  The type of value in the registry.
     * @return An argument builder for the given specs.
     */
    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> registryArg(final String name, final ResourceKey<? extends Registry<T>> key) {
        return Commands.argument(name, RegistryArgument.getOrThrow(key));
    }

    /**
     * Shorthand for creating a resource location argument.
     *
     * @param name The name of the argument node.
     * @return An argument builder for the given specs.
     */
    public static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> idArg(final String name) {
        return Commands.argument(name, ResourceLocationArgument.id());
    }

    /**
     * Shorthand method for creating a {@link HoverEvent} which displays some text.
     *
     * @param txt The text to display on hover.
     * @return A {@link HoverEvent} to display the given text.
     */
    public static HoverEvent displayOnHover(final String txt) {
        return displayOnHover(Component.literal(txt));
    }

    /**
     * Shorthand method for creating a {@link HoverEvent} which displays a text component.
     *
     * @param txt The text component to display on hover.
     * @return A {@link HoverEvent} to display the given component.
     */
    public static HoverEvent displayOnHover(final Component txt) {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, txt);
    }

    /**
     * Shorthand method for creating a {@link ClickEvent} which runs the given command.
     *
     * @param cmd The raw command text to be executed by the command manager.
     * @return A {@link ClickEvent} to run the given command.
     */
    public static ClickEvent clickToRun(final String cmd) {
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd);
    }

    /**
     * Shorthand method for creating a {@link ClickEvent} which opens the given file.
     *
     * @param file The file to be opened when this event runs.
     * @return A {@link ClickEvent} to open the given file.
     */
    public static ClickEvent clickToOpen(final Path file) {
        return new ClickEvent(ClickEvent.Action.OPEN_URL, file.toAbsolutePath().toString());
    }

    /**
     * Shorthand method for creating a {@link ClickEvent} which opens the given URL.
     *
     * @param url The url to be opened when this event runs.
     * @return A {@link ClickEvent} to open the given URL.
     */
    public static ClickEvent clickToGo(final String url) {
        return new ClickEvent(ClickEvent.Action.OPEN_URL, url);
    }

    /**
     * Shorthand method for creating a {@link ClickEvent} which suggests the given command.
     *
     * @param cmd The command to be suggested when this event runs.
     * @return A {@link ClickEvent} to suggest the given command.
     */
    public static ClickEvent clickToSuggest(final String cmd) {
        return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd);
    }

    /**
     * Shorthand method for creating a {@link ClickEvent} which copies the given text.
     *
     * @param txt The text to be copied into the clipboard.
     * @return A {@link ClickEvent} to copy the given text.
     */
    public static ClickEvent clickToCopy(final String txt) {
        return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, txt);
    }

    private static Path getDefaultRoot() {
        final ModDescriptor activeMod = CommandRegistrationContext.getActiveMod();
        return activeMod != null ? activeMod.configFolder() : McUtils.getConfigDir();
    }
}
