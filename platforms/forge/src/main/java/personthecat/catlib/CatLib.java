package personthecat.catlib;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import personthecat.catlib.command.*;
import personthecat.catlib.command.arguments.*;
import personthecat.catlib.command.forge.LibCommandRegistrarImpl;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.event.registry.RegistryAccessEvent;
import personthecat.catlib.event.registry.RegistryAddedEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.FeatureModificationHook;
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.McUtils;

@Mod(LibReference.MOD_ID)
public class CatLib {

    public CatLib() {
        LibConfig.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initCommon);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    @SuppressWarnings("unused")
    private void initCommon(final FMLCommonSetupEvent event) {
        EnumArgument.register();
        FileArgument.register();
        JsonArgument.register();
        PathArgument.register();
        RegistryArgument.register();

        RegistryAccessEvent.EVENT.register(access -> {
            DynamicRegistries.updateRegistries(access);
            RegistryAddedEvent.onRegistryAccess(access);
            FeatureModificationHook.onRegistryAccess(access);
        });

        final CommandRegistrationContext ctx = CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR);
        if (LibConfig.enableGlobalLibCommands()) {
            ctx.addAllCommands(DefaultLibCommands.createAll(LibReference.MOD_DESCRIPTOR, true));
        }
        ctx.addCommand(CatLibCommands.ERROR_MENU).registerAll();

        MinecraftForge.EVENT_BUS.addListener((WorldEvent.Load e) -> {
            if (McUtils.isDedicatedServer()) {
                LibErrorContext.outputServerErrors(false);
            }
            CommonWorldEvent.LOAD.invoker().accept(e.getWorld());
        });
        MinecraftForge.EVENT_BUS.addListener((WorldEvent.Unload e) ->
            CommonWorldEvent.UNLOAD.invoker().accept(e.getWorld()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (McUtils.isClientSide() && LibErrorContext.hasErrors()) {
                LibErrorContext.broadcastErrors(e.getPlayer());
            }
            CommonPlayerEvent.LOGIN.invoker().accept(e.getPlayer(), e.getPlayer().getServer());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) ->
            CommonPlayerEvent.LOGOUT.invoker().accept(e.getPlayer(), e.getPlayer().getServer()));
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        LibCommandRegistrarImpl.copyInto(event.getDispatcher());
    }
}
