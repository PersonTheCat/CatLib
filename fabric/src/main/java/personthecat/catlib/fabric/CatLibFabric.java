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
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.CatLib;
import personthecat.catlib.event.lifecycle.ClientTickEvent;
import personthecat.catlib.event.lifecycle.ServerEvents;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.event.world.fabric.FeatureModificationContextImpl;

@Log4j2
public class CatLibFabric extends CatLib implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        this.init();
        this.commonSetup();
        this.setupBiomeModificationHook();

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
        ServerLifecycleEvents.SERVER_STARTING.register((s) ->
            ServerEvents.LOAD.invoker().accept(s));
        ServerLifecycleEvents.SERVER_STOPPING.register((s) ->
            ServerEvents.UNLOAD.invoker().accept(s));
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc ->
            ClientTickEvent.END.invoker().accept(mc));
    }

    private void setupBiomeModificationHook() {
        for (final var phase : FeatureModificationEvent.Phase.values()) {
            final var id = new ResourceLocation(CatLib.ID, phase.name().toLowerCase());
            final var listener = FeatureModificationEvent.get(phase);
            BiomeModifications.create(id).add(
                convertPhase(phase),
                s -> listener.isValidForBiome(s.getBiomeRegistryEntry()),
                (s, m) -> listener.modifyBiome(new FeatureModificationContextImpl(s, m)));
        }
    }

    private static ModificationPhase convertPhase(FeatureModificationEvent.Phase phase) {
        return switch (phase) {
            case ADDITIONS -> ModificationPhase.ADDITIONS;
            case REMOVALS -> ModificationPhase.REMOVALS;
            case MODIFICATIONS -> ModificationPhase.REPLACEMENTS;
        };
    }

    @Override
    protected <T extends ArgumentType<?>> void registerArgumentType(
            final String id, final Class<T> clazz, final ArgumentTypeInfo<T, ?> info) {
        ArgumentTypeRegistry.registerArgumentType(new ResourceLocation(CatLib.ID, id), clazz, info);
    }
}
