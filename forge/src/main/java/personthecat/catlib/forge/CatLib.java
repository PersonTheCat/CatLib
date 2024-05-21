package personthecat.catlib.forge;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
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
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import personthecat.catlib.command.*;
import personthecat.catlib.command.arguments.*;
import personthecat.catlib.command.forge.LibCommandRegistrarImpl;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.config.forge.LibConfigImpl;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.lifecycle.ClientTickEvent;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.exception.GenericFormattedException;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.event.registry.RegistryAccessEvent;
import personthecat.catlib.event.registry.RegistryAddedEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.forge.FeatureModificationHook;
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.McUtils;
import personthecat.catlib.util.LibUtil;

@Mod(LibReference.MOD_ID)
public class CatLib {

    public CatLib() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        final IEventBus eventBus = MinecraftForge.EVENT_BUS;
        LibConfig.register();
        modBus.addListener(this::initCommon);
        modBus.addListener(this::initClient);
        modBus.addListener(this::initRegistries);
        eventBus.addListener(this::shutdown);
        eventBus.addListener(this::registerCommands);
    }

    private void initCommon(final FMLCommonSetupEvent event) {
        LibConfigImpl.updateJsonContext();
        if (LibConfig.enableTestError()) {
            LibErrorContext.error(LibReference.MOD_DESCRIPTOR,
                new GenericFormattedException(new RuntimeException("test error"), "tooltip working!"));
        }
        RegistryAccessEvent.EVENT.register(access -> {
            DynamicRegistries.updateRegistries(access);
            RegistryAddedEvent.onRegistryAccess(access);
        });
        final CommandRegistrationContext ctx = CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR);
        if (LibConfig.enableCatlibCommands()) {
            ctx.addLibCommands();
        }
        ctx.addCommand(CatLibCommands.ERROR_MENU).registerAll();

        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Load e) -> {
            if (McUtils.isDedicatedServer()) {
                LibErrorContext.outputServerErrors(false);
            }
            CommonWorldEvent.LOAD.invoker().accept(e.getLevel());
        });
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload e) ->
            CommonWorldEvent.UNLOAD.invoker().accept(e.getLevel()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (McUtils.isClientSide() && LibErrorContext.hasErrors()) {
                LibErrorContext.broadcastErrors(e.getEntity());
            }
            CommonPlayerEvent.LOGIN.invoker().accept(e.getEntity(), e.getEntity().getServer());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) ->
            CommonPlayerEvent.LOGOUT.invoker().accept(e.getEntity(), e.getEntity().getServer()));
    }

    private void initClient(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) ->
            ClientTickEvent.END.invoker().accept(Minecraft.getInstance()));
    }

    private void initRegistries(final RegisterEvent event) {
        event.register(Registries.COMMAND_ARGUMENT_TYPE, helper -> {
            this.registerArgumentType(
                helper, "enum_argument", LibUtil.asParentType(EnumArgument.class),
                EnumArgument.INFO);
            this.registerArgumentType(
                helper, "file_argument", FileArgument.class,
                FileArgument.INFO);
            this.registerArgumentType(
                helper, "json_argument", JsonArgument.class,
                JsonArgument.INFO);
            this.registerArgumentType(
                helper, "path_argument", PathArgument.class,
                SingletonArgumentInfo.contextFree(PathArgument::new));
            this.registerArgumentType(
                helper, "registry_argument", LibUtil.asParentType(RegistryArgument.class),
                RegistryArgument.INFO);
        });
        event.register(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, helper ->
            helper.register(
                new ResourceLocation(LibReference.MOD_ID, "none_biome_modifier"),
                FeatureModificationHook.CODEC));
    }

    private <T extends ArgumentType<?>> void registerArgumentType(
            final RegisterEvent.RegisterHelper<ArgumentTypeInfo<?, ?>> helper,
            final String id,
            final Class<T> clazz,
            final ArgumentTypeInfo<T, ?> info) {
        helper.register(new ResourceLocation(LibReference.MOD_ID, id), info);
        ArgumentTypeInfos.registerByClass(clazz, info);
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        LibCommandRegistrarImpl.copyInto(event.getDispatcher());
    }

    private void shutdown(final ServerStoppedEvent _event) {
        DynamicRegistries.onSeverClosed();
    }
}
