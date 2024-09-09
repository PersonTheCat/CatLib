package personthecat.catlib.command.fabric;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import personthecat.catlib.command.CommandSide;

public final class LibCommandRegistrarImpl {

    private LibCommandRegistrarImpl() {}

    public static void registerCommand(final LiteralArgumentBuilder<CommandSourceStack> cmd, final CommandSide side) {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            if (side.canRegister(environment == Commands.CommandSelection.DEDICATED)) dispatcher.register(cmd);
        });
    }
}
