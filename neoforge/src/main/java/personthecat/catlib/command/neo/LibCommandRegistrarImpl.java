package personthecat.catlib.command.neo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.experimental.UtilityClass;
import net.minecraft.commands.CommandSourceStack;
import personthecat.catlib.command.CommandSide;
import personthecat.catlib.util.neo.McUtilsImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class LibCommandRegistrarImpl {

    private static final Map<LiteralArgumentBuilder<CommandSourceStack>, CommandSide> COMMANDS = new ConcurrentHashMap<>();

    public static void registerCommand(final LiteralArgumentBuilder<CommandSourceStack> cmd, final CommandSide side) {
        COMMANDS.put(cmd, side);
    }

    public static void copyInto(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final boolean dedicated = McUtilsImpl.isDedicatedServer();
        COMMANDS.forEach((cmd, side) -> {
            if (side.canRegister(dedicated)) dispatcher.register(cmd);
        });
    }
}
