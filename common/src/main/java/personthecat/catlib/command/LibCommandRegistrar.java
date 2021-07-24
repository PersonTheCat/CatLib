package personthecat.catlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustInherit;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

import java.util.List;

@OverwriteTarget
public class LibCommandRegistrar {

    @PlatformMustInherit
    public static void registerCommands(final ModDescriptor mod, final boolean libCommands, final Class<?>... types) {
        final CommandRegistrationContext ctx = CommandRegistrationContext.forMod(mod);
        CommandClassEvaluator.getBuilders(types).forEach(ctx::addCommand);
        if (libCommands) ctx.addLibCommands();
        ctx.registerAll();
    }

    @PlatformMustInherit
    public static void registerCommand(final LiteralArgumentBuilder<CommandSourceStack> cmd) {
        registerCommand(cmd, CommandSide.EITHER);
    }

    @PlatformMustOverwrite
    public static void registerCommand(final LiteralArgumentBuilder<CommandSourceStack> cmd, final CommandSide side) {
        throw new MissingOverrideException();
    }
}
