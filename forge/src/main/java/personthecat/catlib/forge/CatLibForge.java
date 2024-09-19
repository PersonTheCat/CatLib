package personthecat.catlib.forge;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import personthecat.catlib.CatLib;
import personthecat.catlib.command.forge.LibCommandRegistrarImpl;
import personthecat.catlib.event.lifecycle.ClientTickEvent;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.forge.FeatureModificationHook;

@Mod(CatLib.ID)
public class CatLibForge extends CatLib {
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
        DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, CatLib.ID);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
        DeferredRegister.create(ForgeRegistries.BIOME_MODIFIER_SERIALIZERS, CatLib.ID);

    public CatLibForge() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        final IEventBus eventBus = MinecraftForge.EVENT_BUS;
        ARGUMENT_TYPES.register(modBus);
        BIOME_MODIFIERS.register(modBus);

        this.init();
        BIOME_MODIFIERS.register("none_biome_modifier", () -> FeatureModificationHook.CODEC);
        modBus.addListener((FMLCommonSetupEvent e) -> {
            this.setupCommonTranslations();
            this.commonSetup();
        });
        modBus.addListener((FMLClientSetupEvent e) -> this.setupClientTranslations());
        eventBus.addListener((ServerStoppedEvent e) ->
            this.shutdown());
        eventBus.addListener((RegisterCommandsEvent e) ->
            LibCommandRegistrarImpl.copyInto(e.getDispatcher()));
    }

    private void setupCommonTranslations() {
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Load e) ->
            CommonWorldEvent.LOAD.invoker().accept(e.getLevel()));
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload e) ->
            CommonWorldEvent.UNLOAD.invoker().accept(e.getLevel()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) ->
            CommonPlayerEvent.LOGIN.invoker().accept(e.getEntity(), e.getEntity().getServer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) ->
            CommonPlayerEvent.LOGOUT.invoker().accept(e.getEntity(), e.getEntity().getServer()));
    }

    private void setupClientTranslations() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) ->
            ClientTickEvent.END.invoker().accept(Minecraft.getInstance()));
    }

    @Override
    protected <T extends ArgumentType<?>> void registerArgumentType(
            final String id, final Class<T> clazz, final ArgumentTypeInfo<T, ?> info) {
        ARGUMENT_TYPES.register(id, () -> info);
        ArgumentTypeInfos.registerByClass(clazz, info);
    }
}
