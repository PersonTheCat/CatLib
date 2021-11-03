package personthecat.catlib;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import personthecat.catlib.command.*;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.command.annotations.Node;
import personthecat.catlib.command.arguments.*;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.registry.DynamicRegistries;
import personthecat.catlib.event.registry.RegistryAccessEvent;
import personthecat.catlib.event.registry.RegistryAddedEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.FeatureModificationHook;
import personthecat.catlib.util.LibReference;

@Mod(LibReference.MOD_ID)
public class CatLib {

    public CatLib() {
        LibConfig.register(ModLoadingContext.get().getActiveContainer());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initCommon);
        MinecraftForge.EVENT_BUS.addListener(this::initServer);
    }

    @SuppressWarnings("unused")
    private void initCommon(final FMLCommonSetupEvent event) {
        EnumArgument.register();
        FileArgument.register();
        HjsonArgument.register();
        PathArgument.register();
        RegistryArgument.register();

        RegistryAccessEvent.EVENT.register(access -> {
            DynamicRegistries.updateRegistries(access);
            RegistryAddedEvent.onRegistryAccess(access);
            FeatureModificationHook.onRegistryAccess(access);
        });

//        if (LibConfig.enableGlobalLibCommands()) {
//            CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR)
//                .addAllCommands(DefaultLibCommands.createAll(LibReference.MOD_DESCRIPTOR, true))
//                .registerAll();
//        }

        CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR)
            .addAllCommands(Commands.class)
            .registerAll();

        MinecraftForge.EVENT_BUS.addListener((WorldEvent.Load e) ->
            CommonWorldEvent.LOAD.invoker().accept(e.getWorld()));
        MinecraftForge.EVENT_BUS.addListener((WorldEvent.Unload e) ->
            CommonWorldEvent.UNLOAD.invoker().accept(e.getWorld()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) ->
            CommonPlayerEvent.LOGIN.invoker().accept(e.getPlayer(), e.getPlayer().getServer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) ->
            CommonPlayerEvent.LOGOUT.invoker().accept(e.getPlayer(), e.getPlayer().getServer()));
    }

    private void initServer(final FMLServerStartingEvent event) {
        LibCommandRegistrar.copyInto(event.getServer().getCommands());
    }

    private static class Commands {

        @ModCommand
        private static void oneTwoThree(final CommandContextWrapper ctx) {}

        @ModCommand(name = "four", branch = @Node("six"))
        private static void fourFiveSix(final CommandContextWrapper ctx) {}

        @ModCommand(branch = @Node("nine"))
        private static void sevenEightNine(final CommandContextWrapper ctx) {}

        @ModCommand
        private void deeEeeEff(final CommandContextWrapper ctx) {}

        @ModCommand(branch = @Node(name = "twelve", isBoolean = true))
        private static void tenElevenTwelve(final CommandContextWrapper ctx) {}

        @ModCommand(branch = @Node("dee"))
        private static void ayBeeCee(final CommandContextWrapper ctx) {}
    }
}
