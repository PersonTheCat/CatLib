package personthecat.catlib.neo;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import personthecat.catlib.CatLib;
import personthecat.catlib.command.neo.LibCommandRegistrarImpl;
import personthecat.catlib.event.lifecycle.ClientTickEvent;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.neo.FeatureModificationHook;

@Mod(CatLib.ID)
public class CatLibNeo extends CatLib {
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
        DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, CatLib.ID);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
        DeferredRegister.create(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, CatLib.ID);

    public CatLibNeo(final IEventBus modBus) {
        final IEventBus eventBus = NeoForge.EVENT_BUS;
        ARGUMENT_TYPES.register(modBus);
        BIOME_MODIFIERS.register(modBus);

        this.init();
        BIOME_MODIFIERS.register("none_biome_modifier", () -> FeatureModificationHook.CODEC);
        modBus.addListener((FMLCommonSetupEvent e) -> {
            this.setupCommonTranslations();
            this.commonSetup();
        });
        modBus.addListener((FMLClientSetupEvent e) ->
            this.setupClientTranslations());
        eventBus.addListener((ServerStoppedEvent e) ->
            this.shutdown());
        eventBus.addListener((RegisterCommandsEvent e) ->
            LibCommandRegistrarImpl.copyInto(e.getDispatcher()));
    }

    private void setupCommonTranslations() {
        NeoForge.EVENT_BUS.addListener((LevelEvent.Load e) ->
            CommonWorldEvent.LOAD.invoker().accept(e.getLevel()));
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload e) ->
            CommonWorldEvent.UNLOAD.invoker().accept(e.getLevel()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) ->
            CommonPlayerEvent.LOGIN.invoker().accept(e.getEntity(), e.getEntity().getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) ->
            CommonPlayerEvent.LOGOUT.invoker().accept(e.getEntity(), e.getEntity().getServer()));
    }

    private void setupClientTranslations() {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) ->
            ClientTickEvent.END.invoker().accept(Minecraft.getInstance()));
    }

    @Override
    protected <T extends ArgumentType<?>> void registerArgumentType(
            final String id, final Class<T> clazz, final ArgumentTypeInfo<T, ?> info) {
        ARGUMENT_TYPES.register(id, () -> info);
        ArgumentTypeInfos.registerByClass(clazz, info);
    }
}
