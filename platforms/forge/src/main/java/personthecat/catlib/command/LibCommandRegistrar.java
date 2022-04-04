package personthecat.catlib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.experimental.UtilityClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import personthecat.catlib.util.McUtils;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
@OverwriteClass
@InheritMissingMembers
public class LibCommandRegistrar {

    private static final Map<LiteralArgumentBuilder<CommandSourceStack>, CommandSide> COMMANDS = new ConcurrentHashMap<>();

    @Overwrite
    @SuppressWarnings("unused")
    public static void registerCommand(final LiteralArgumentBuilder<CommandSourceStack> cmd, final CommandSide side) {
        COMMANDS.put(cmd, side);
    }

    public static void copyInto(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final boolean dedicated = McUtils.isDedicatedServer();
        COMMANDS.forEach((cmd, side) -> {
            if (side.canRegister(dedicated)) dispatcher.register(cmd);
        });
    }
}
