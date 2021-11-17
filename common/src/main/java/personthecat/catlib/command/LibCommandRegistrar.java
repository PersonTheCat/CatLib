package personthecat.catlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.experimental.UtilityClass;
import net.minecraft.commands.CommandSourceStack;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustInherit;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

import java.util.List;

/**
 * A platform-agnostic registrar for regular Brigadier command builders.
 */
@UtilityClass
@OverwriteTarget
public class LibCommandRegistrar {

    /**
     * Registers every command in the given class files to be applied as soon as
     * the server loads. For more control over this process, consider using a
     * {@link CommandRegistrationContext} directly.
     *
     * @param mod The current mod's descriptor, to be used in generating argument nodes.
     * @param libCommands Whether to additionally register every command provided by the
     *                    library directly to this mod's main command node.
     * @param types Every class containing annotated command methods.
     */
    @PlatformMustInherit
    public static void registerCommands(final ModDescriptor mod, final boolean libCommands, final Class<?>... types) {
        final CommandRegistrationContext ctx = CommandRegistrationContext.forMod(mod);
        CommandClassEvaluator.getBuilders(mod, types).forEach(ctx::addCommand);
        if (libCommands) ctx.addLibCommands();
        ctx.registerAll();
    }

    /**
     * Registers a generic {@link LiteralArgumentBuilder} to be supplied either on the
     * dedicated server or the integrated server side.
     *
     * @param cmd The standard Brigadier command being registered.
     */
    @PlatformMustInherit
    public static void registerCommand(final LiteralArgumentBuilder<CommandSourceStack> cmd) {
        registerCommand(cmd, CommandSide.EITHER);
    }

    /**
     * Registers a generic {@link LiteralArgumentBuilder} to be supplied on a specific
     * server side.
     *
     * @param cmd The standard Brigadier command being registered.
     * @param side The specific server side on which this command can be supplied.
     */
    @PlatformMustOverwrite
    public static void registerCommand(final LiteralArgumentBuilder<CommandSourceStack> cmd, final CommandSide side) {
        throw new MissingOverrideException();
    }
}
