package personthecat.catlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import personthecat.catlib.util.McTools;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OverwriteClass
@InheritMissingMembers
public class LibCommandRegistrar {

    private static final Map<LiteralArgumentBuilder<CommandSourceStack>, CommandSide> COMMANDS = new ConcurrentHashMap<>();

    @Overwrite
    @SuppressWarnings("unused")
    public static void registerCommand(final LiteralArgumentBuilder<CommandSourceStack> cmd, final CommandSide side) {
        COMMANDS.put(cmd, side);
    }

    public static void copyInto(final Commands manager) {
        final boolean dedicated = McTools.isDedicatedServer();
        COMMANDS.forEach((cmd, side) -> {
            if (side.canRegister(dedicated)) manager.getDispatcher().register(cmd);
        });
    }
}
