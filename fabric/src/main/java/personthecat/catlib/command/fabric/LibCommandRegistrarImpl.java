package personthecat.catlib.command.fabric;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import personthecat.catlib.command.CommandSide;

@UtilityClass
public class LibCommandRegistrarImpl {

    public static void registerCommand(final LiteralArgumentBuilder<CommandSourceStack> cmd, final CommandSide side) {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (side.canRegister(dedicated)) dispatcher.register(cmd);
        });
    }
}
