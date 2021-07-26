package personthecat.catlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@UtilityClass
@OverwriteClass
@InheritMissingMembers
public class LibCommandRegistrar {

    @Overwrite
    public static void registerCommand(final LiteralArgumentBuilder<CommandSourceStack> cmd, final CommandSide side) {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (side.canRegister(dedicated)) dispatcher.register(cmd);
        });
    }
}
