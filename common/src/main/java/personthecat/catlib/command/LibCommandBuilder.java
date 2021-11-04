package personthecat.catlib.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.command.function.CommandFunction;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.SyntaxLinter;
import personthecat.fresult.Result;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A manual builder type used for generating argument nodes with automatically-generated
 * custom help entries. In addition to providing a convenient, platform-agnostic way to
 * control the type of command and accepted server side on which to register the command,
 * this wrapper will take care of handling errors and can provide additional utilities to
 * the command definitions via {@link CommandContextWrapper} which are not already
 * by the regular {@link CommandContext}.
 * <p>
 *   To begin using this wrapper, start by creating a {@link Template} as follows:
 * </p><pre>
 *     LibCommandBuilder.named("myCommand")
 * </pre><p>
 *   The template returned by this method provides a series of options which can be used
 *   the configure the generated help command, supply mod information, and more.
 * </p><pre>
 *     LibCommandBuilder.named("myCommand")
 *
 *         // Append text to the generated help message.
 *         .append("Runs a demo command")
 *
 *         // An optional linter used for syntax highlighting in the chat.
 *         .linter(new MyLinter())
 *
 *         // Only run this on a dedicated server
 *         .side(CommandSide.DEDICATED)
 *
 *         // Optionally wrap a throwing consumer of type {@link CommandContextWrapper}.
 *         // This converts the wrapper to an int function of type {@link CommandContext}.
 *         .wrap("", wrapper -> wrapper.sendMessage("Hello, world!"))
 *         ...
 * </pre><p>
 *   Finally, once the command's metadata have been configured, you can return a complete
 *   command builder by calling {@link Template#generate}.
 * </p><pre>
 *     final LibCommandBuilder cmd = LibCommandBuilder.named("myCommand")
 *         ...
 *         // This method exposes the raw Brigadier argument tree.
 *         .generate((builder, wrappers) -> builder.execute(wrappers.get("")));
 * </pre><p>
 *   Generated builder objects can then be passed into an active {@link CommandRegistrationContext}
 *   or annotated with {@link personthecat.catlib.command.annotations.CommandBuilder} and passed in through the parent class.
 * </p>
 */
@Log4j2
@AllArgsConstructor
@ParametersAreNonnullByDefault
public final class LibCommandBuilder {
    private final LiteralArgumentBuilder<CommandSourceStack> command;
    private final HelpCommandInfo info;
    private final CommandType type;
    private final CommandSide side;

    /**
     * Constructs a new builder template using the only piece of required
     * information for creating a command: it's name!
     *
     * @param name The literal argument required to run the command.
     * @return A new template used to customize the command.
     */
    public static Template named(final String name) {
        return new Template(name);
    }

    public LiteralArgumentBuilder<CommandSourceStack> getCommand() {
        return this.command;
    }

    public HelpCommandInfo getHelpInfo() {
        return this.info;
    }

    public CommandType getType() {
        return this.type;
    }

    public CommandSide getSide() {
        return this.side;
    }

    public static class Template {
        private final CommandMapBuilder wrappers;
        private final String name;
        private String arguments;
        private String description;
        private CommandType type;
        private CommandSide side;

        @Nullable
        private SyntaxLinter linter;

        @Nullable
        private ModDescriptor mod;

        private Template(final String name) {
            this.wrappers = new CommandMapBuilder();
            this.name = name;
            this.arguments = "";
            this.description = "";
            this.type = CommandType.MOD;
            this.side = CommandSide.EITHER;
        }

        /**
         * Sets the optional subtext to be displayed after the command name on the
         * current mod's generated help page.
         *
         * @param arguments The subtext of the command, usually its arguments.
         * @return <code>this</code>, for method chaining.
         */
        public Template arguments(final String arguments) {
            this.arguments = arguments;
            return this;
        }

        /**
         * Enables this command's generated help entry and Sets the entire description
         * to be displayed on the mod's help page.
         *
         * @param description The automatically formatted help text.
         * @return <code>this</code>, for method chaining.
         */
        public Template description(final String description) {
            this.description = description;
            return this;
        }

        /**
         * Appends additional text to this command's help entry, if needed.
         * <p>
         *   Note that this does not affect how the entry will be formatted, as that
         *   can be configured automatically via {@link CommandRegistrationContext}.
         * </p>
         *
         * @param description The additional description text to append.
         * @return <code>this</code>, for method chaining.
         */
        public Template append(final String description) {
            this.description += " " + description;
            return this;
        }

        /**
         * The type of command to generate. Either a sub command of the mod's main
         * command, or a global command attached to the root command node.
         *
         * @param type The type of command being generated.
         * @return <code>this</code>, for method chaining.
         */
        public Template type(final CommandType type) {
            this.type = type;
            return this;
        }

        /**
         * The type of server side required for this command to be registered.
         *
         * @param side Whichever side the command should run on.
         * @return <code>this</code>, for method chaining.
         */
        public Template side(final CommandSide side) {
            this.side = side;
            return this;
        }

        /**
         * An optional linter which can be used to highlight text in the chat.
         * This will be provided to the exit nodes via {@link CommandContextWrapper}.
         *
         * @param linter The syntax highlighter used by the context wrapper.
         * @return <code>this</code>, for method chaining.
         */
        public Template linter(final SyntaxLinter linter) {
            this.linter = linter;
            return this;
        }

        /**
         * Configures this command's mod descriptor which will be used by the
         * registration context and certain argument types to provide a customized
         * experience to the command function.
         * <p>
         *   Note that this step is unnecessary if a {@link CommandRegistrationContext}
         *   has already been created and has not yet been disposed of.
         * </p>
         *
         * @param mod The current mod's descriptor.
         * @return <code>this</code>, for method chaining.
         */
        public Template mod(final ModDescriptor mod) {
            this.mod = mod;
            return this;
        }

        /**
         * Converts a consumer of type {@link CommandContextWrapper} into a standard
         * {@link Command} function.
         *
         * @param key The key used to retrieve this command when calling {@link #generate}.
         * @param endpoint The actual method being executed by the command.
         * @return <code>this</code>, for method chaining.
         */
        public Template wrap(final String key, final CommandFunction endpoint) {
            this.wrappers.put(key, endpoint);
            return this;
        }

        /**
         * Generates everything needed for the accepting {@link CommandRegistrationContext}
         * to build and register this command.
         *
         * @param generator A bi-consumer providing access to the underlying argument node
         *                  and wrapped command functions.
         * @return A fully-constructed command builder.
         */
        public LibCommandBuilder generate(final CommandGenerator<CommandSourceStack> generator) {
            if (this.mod == null) this.mod = CommandRegistrationContext.getActiveModOrThrow();
            if (this.linter == null) this.linter = this.mod.getDefaultLinter();

            final LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(this.name);
            final BuilderUtil util = new BuilderUtil(this.wrappers, this.linter, this.mod);
            final HelpCommandInfo helpInfo = new HelpCommandInfo(this.name, this.arguments, this.description, this.type);

            final LiteralArgumentBuilder<CommandSourceStack> cmd = generator.apply(builder, util);
            return new LibCommandBuilder(cmd, helpInfo, this.type, this.side);
        }
    }

    private static class CommandMapBuilder extends HashMap<String, CommandFunction> {}

    @AllArgsConstructor
    public static class BuilderUtil {

        private final Map<String, CommandFunction> map;
        private final SyntaxLinter linter;
        private final ModDescriptor mod;

        public Command<CommandSourceStack> get(final String key) {
            final CommandFunction fn = this.map.get(key);
            Objects.requireNonNull(fn, "No command function for key: " + key);

            return ctx -> this.wrapCommand(new CommandContextWrapper(ctx, this.linter, this.mod), fn);
        }

        public Command<CommandSourceStack> wrap(final CommandFunction fn) {
            return ctx -> this.wrapCommand(new CommandContextWrapper(ctx, this.linter, this.mod), fn);
        }

        private int wrapCommand(final CommandContextWrapper wrapper, final CommandFunction fn) {
            return Result.suppress(() -> fn.execute(wrapper))
                .ifErr(e -> this.handleException(wrapper, e))
                .fold(v -> 1, e -> -1);
        }

        private void handleException(final CommandContextWrapper wrapper, final Throwable e) {
            // There will always be an ITE if the method was invoked reflectively.
            if (e instanceof InvocationTargetException && e.getCause() != null) {
                this.handleException(wrapper, e.getCause());
                return;
            }
            log.warn("Error running command: {}\nStacktrace:\n{}", e.getMessage(), e.getStackTrace());
            wrapper.sendError("{}: {}", e.getClass().getSimpleName(), e.getMessage());

            Throwable cause = e;
            while ((cause = cause.getCause()) != null) {
                log.warn("Caused by: {}\nStacktrace:\n{}", cause.getMessage(), cause.getStackTrace());
                wrapper.sendError(cause.getMessage());
            }
        }
    }

    @FunctionalInterface
    public interface CommandGenerator<S> {
        LiteralArgumentBuilder<S> apply(final LiteralArgumentBuilder<S> builder, final BuilderUtil wrappers);
    }
}
