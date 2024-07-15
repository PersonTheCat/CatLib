package personthecat.catlib.fabric;

import com.mojang.brigadier.arguments.ArgumentType;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.CatLib;
import personthecat.catlib.event.lifecycle.ClientTickEvent;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.registry.RegistryMapSource;
import personthecat.catlib.event.registry.DataRegistryEvent;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.fabric.FeatureModificationContextImpl;
import personthecat.catlib.event.world.FeatureModificationEvent;

@Log4j2
public class CatLibFabric extends CatLib implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        this.init();
        this.commonSetup();
        this.setupBiomeModificationHook();

        DynamicRegistrySetupCallback.EVENT.register(c ->
            DataRegistryEvent.PRE.invoker().accept(new RegistryMapSource(c.stream())));
        ServerWorldEvents.LOAD.register((s, l) ->
            CommonWorldEvent.LOAD.invoker().accept(l));
        ServerWorldEvents.UNLOAD.register((s, l) ->
            CommonWorldEvent.UNLOAD.invoker().accept(l));
        ServerPlayConnectionEvents.JOIN.register((h, tx, s) ->
            CommonPlayerEvent.LOGIN.invoker().accept(h.player, s));
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) ->
            CommonPlayerEvent.LOGOUT.invoker().accept(h.player, s));
        ServerLifecycleEvents.SERVER_STOPPED.register((s) ->
            this.shutdown());
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc ->
            ClientTickEvent.END.invoker().accept(mc));
    }

    private void setupBiomeModificationHook() {
        BiomeModifications.create(new ResourceLocation(CatLib.ID, "biome_updates"))
            .add(ModificationPhase.REMOVALS, s -> FeatureModificationEvent.hasEvent(s.getBiomeKey().location()), (s, m) -> {
                final FeatureModificationContext ctx = new FeatureModificationContextImpl(s, m);
                FeatureModificationEvent.global().invoker().accept(ctx);
                FeatureModificationEvent.get(s.getBiomeKey().location()).accept(ctx);
            });
    }

    @Override
    protected <T extends ArgumentType<?>> void registerArgumentType(
            final String id, final Class<T> clazz, final ArgumentTypeInfo<T, ?> info) {
        ArgumentTypeRegistry.registerArgumentType(new ResourceLocation(CatLib.ID, id), clazz, info);
    }
}
