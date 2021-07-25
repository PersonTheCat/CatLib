package personthecat.catlib.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.util.SyntaxLinter;
import personthecat.fresult.Result;
import personthecat.fresult.functions.ThrowingConsumer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Log4j2
@AllArgsConstructor
@ParametersAreNonnullByDefault
public final class LibCommandBuilder {
    private final LiteralArgumentBuilder<CommandSourceStack> command;
    private final HelpCommandInfo info;
    private final CommandType type;
    private final CommandSide side;

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
            this.linter = SyntaxLinter.DEFAULT_LINTER;
        }

        public Template arguments(final String arguments) {
            this.arguments = arguments;
            return this;
        }

        public Template description(final String description) {
            this.description = description;
            return this;
        }

        public Template append(final String description) {
            this.description += " " + description;
            return this;
        }

        public Template type(final CommandType type) {
            this.type = type;
            return this;
        }

        public Template side(final CommandSide side) {
            this.side = side;
            return this;
        }

        public Template linter(final SyntaxLinter linter) {
            this.linter = linter;
            return this;
        }

        public Template mod(final ModDescriptor mod) {
            this.mod = mod;
            return this;
        }

        public Template wrap(final String key, final ThrowingConsumer<CommandContextWrapper, Throwable> endpoint) {
            this.wrappers.put(key, endpoint);
            return this;
        }

        public LibCommandBuilder generate(final CommandFunction<CommandSourceStack> generator) {
            if (this.mod == null) this.mod = CommandRegistrationContext.getActiveModOrThrow();

            final LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(this.name);
            final CommandMap commandMap = new CommandMap(this.wrappers, this.linter, this.mod);
            final HelpCommandInfo helpInfo = new HelpCommandInfo(this.name, this.arguments, this.description, this.type);

            final LiteralArgumentBuilder<CommandSourceStack> cmd = generator.apply(builder, commandMap);
            return new LibCommandBuilder(cmd, helpInfo, this.type, this.side);
        }
    }

    private static class CommandMapBuilder extends HashMap<String, ThrowingConsumer<CommandContextWrapper, Throwable>> {}

    @AllArgsConstructor
    public static class CommandMap {

        private final Map<String, ThrowingConsumer<CommandContextWrapper, Throwable>> map;
        private final SyntaxLinter linter;
        private final ModDescriptor mod;

        public Command<CommandSourceStack> get(final String key) {
            final ThrowingConsumer<CommandContextWrapper, Throwable> fn = this.map.get(key);
            Objects.requireNonNull(fn, "No command function for key: " + key);

            return ctx -> this.wrapCommand(new CommandContextWrapper(ctx, this.linter, this.mod), fn);
        }

        private int wrapCommand(final CommandContextWrapper wrapper, final ThrowingConsumer<CommandContextWrapper, Throwable> fn) {
            return Result.suppress(() -> fn.accept(wrapper))
                .ifErr(e -> this.handleException(wrapper, e))
                .map(v -> 1)
                .orElse(-1);
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
    public interface CommandFunction<S> {
        LiteralArgumentBuilder<S> apply(final LiteralArgumentBuilder<S> builder, final CommandMap wrappers);
    }
}
