package personthecat.catlib;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import personthecat.catlib.command.*;
import personthecat.catlib.command.arguments.FileArgument;
import personthecat.catlib.command.arguments.HjsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.util.LibReference;

@Mod(LibReference.MOD_ID)
public class CatLib {

    public CatLib() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initCommon);
        MinecraftForge.EVENT_BUS.addListener(this::initServer);
    }

    @SuppressWarnings("unused")
    private void initCommon(final FMLCommonSetupEvent event) {
        FileArgument.register();
        HjsonArgument.register();
        PathArgument.register();

        if (LibConfig.enableGlobalLibCommands.get()) {
            final CommandRegistrationContext ctx = CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR);
            DefaultLibCommands.createAll(LibReference.MOD_DESCRIPTOR, true).forEach(ctx::addCommand);
            ctx.registerAll();
        }
    }

    @SuppressWarnings("unused")
    private void initServer(final FMLServerStartingEvent event) {
        LibCommandRegistrar.copyInto(event.getServer().getCommands());
    }
}
