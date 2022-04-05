package personthecat.catlib;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.command.CatLibCommands;
import personthecat.catlib.command.CommandRegistrationContext;
import personthecat.catlib.command.DefaultLibCommands;
import personthecat.catlib.command.arguments.*;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.registry.DynamicRegistries;
import personthecat.catlib.event.registry.RegistryAccessEvent;
import personthecat.catlib.event.registry.RegistryAddedEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.fabric.FeatureModificationContextImpl;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.McUtils;

public class CatLib implements ModInitializer {

    @Override
    public void onInitialize() {
        LibConfig.register();

        EnumArgument.register();
        FileArgument.register();
        HjsonArgument.register();
        PathArgument.register();
        RegistryArgument.register();

        this.setupBiomeModificationHook();

        RegistryAccessEvent.EVENT.register(access -> {
            DynamicRegistries.updateRegistries(access);
            RegistryAddedEvent.onRegistryAccess(access);
        });

        final CommandRegistrationContext ctx = CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR);
        if (LibConfig.enableGlobalLibCommands()) {
            ctx.addAllCommands(DefaultLibCommands.createAll(LibReference.MOD_DESCRIPTOR, true));
        }
        ctx.addCommand(CatLibCommands.ERROR_MENU).registerAll();

        ServerWorldEvents.LOAD.register((s, l) -> {
            if (McUtils.isDedicatedServer()) {
                LibErrorContext.outputServerErrors(false);
            }
            CommonWorldEvent.LOAD.invoker().accept(l);
        });
        ServerWorldEvents.UNLOAD.register((s, l) -> CommonWorldEvent.UNLOAD.invoker().accept(l));
        ServerPlayConnectionEvents.JOIN.register((h, tx, s) -> {
            if (McUtils.isClientSide() && LibErrorContext.hasErrors()) {
                LibErrorContext.broadcastErrors(h.player);
            }
            CommonPlayerEvent.LOGIN.invoker().accept(h.player, s);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> CommonPlayerEvent.LOGOUT.invoker().accept(h.player, s));
    }

    private void setupBiomeModificationHook() {
        BiomeModifications.create(new ResourceLocation(LibReference.MOD_ID, "biome_updates"))
            .add(ModificationPhase.REMOVALS, s -> true, (s, m) -> {
                FeatureModificationEvent.EVENT.invoker().accept(new FeatureModificationContextImpl(s, m));
            });
    }
}
