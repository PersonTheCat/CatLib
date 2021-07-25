package personthecat.catlib;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import personthecat.catlib.command.CommandRegistrationContext;
import personthecat.catlib.command.DefaultLibCommands;
import personthecat.catlib.command.arguments.FileArgument;
import personthecat.catlib.command.arguments.HjsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.util.LibReference;

public class CatLib implements ModInitializer {

    @Override
    public void onInitialize() {
        AutoConfig.register(LibConfig.class, Toml4jConfigSerializer::new);

        FileArgument.register();
        HjsonArgument.register();
        PathArgument.register();

        if (LibConfig.enableGlobalLibCommands.get()) {
            final CommandRegistrationContext ctx = CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR);
            DefaultLibCommands.createAll(LibReference.MOD_DESCRIPTOR, true).forEach(ctx::addCommand);
            ctx.registerAll();
        }
    }
}
