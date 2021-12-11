package personthecat.catlib;

import net.minecraft.Util;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import personthecat.catlib.command.*;
import personthecat.catlib.command.arguments.*;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.event.error.LibErrorContext;
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

        final CommandRegistrationContext ctx = CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR);
        if (LibConfig.enableGlobalLibCommands()) {
            ctx.addAllCommands(DefaultLibCommands.createAll(LibReference.MOD_DESCRIPTOR, true));
        }
        ctx.addCommand(CatLibCommands.ERROR_MENU).registerAll();

        MinecraftForge.EVENT_BUS.addListener((WorldEvent.Load e) ->
            CommonWorldEvent.LOAD.invoker().accept(e.getWorld()));
        MinecraftForge.EVENT_BUS.addListener((WorldEvent.Unload e) ->
            CommonWorldEvent.UNLOAD.invoker().accept(e.getWorld()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (LibErrorContext.hasErrors()) {
                e.getPlayer().sendMessage(new TranslatableComponent("catlib.errorText.clickHere")
                    .withStyle(Style.EMPTY.withClickEvent(CommandUtils.clickToRun("/catlib errors"))), Util.NIL_UUID);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) ->
            CommonPlayerEvent.LOGIN.invoker().accept(e.getPlayer(), e.getPlayer().getServer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) ->
            CommonPlayerEvent.LOGOUT.invoker().accept(e.getPlayer(), e.getPlayer().getServer()));
    }

    private void initServer(final FMLServerStartingEvent event) {
        LibCommandRegistrar.copyInto(event.getServer().getCommands());
    }
}
