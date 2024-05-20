package personthecat.catlib.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.command.CatLibCommands;
import personthecat.catlib.command.CommandRegistrationContext;
import personthecat.catlib.command.arguments.*;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.lifecycle.ClientTickEvent;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.exception.GenericFormattedException;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.event.registry.RegistryAccessEvent;
import personthecat.catlib.event.registry.RegistryAddedEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.fabric.FeatureModificationContextImpl;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.util.LibReference;
import personthecat.catlib.util.McUtils;
import personthecat.catlib.util.LibUtil;

public class CatLib implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        LibConfig.register();
        this.registerArgumentTypes();

        this.setupBiomeModificationHook();

        LibErrorContext.error(LibReference.MOD_DESCRIPTOR, new GenericFormattedException(new RuntimeException("hello, world"), "tooltip working!"));

        RegistryAccessEvent.EVENT.register(access -> {
            DynamicRegistries.updateRegistries(access);
            RegistryAddedEvent.onRegistryAccess(access);
        });

        final CommandRegistrationContext ctx = CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR);
        if (LibConfig.enableCatlibCommands()) {
            ctx.addLibCommands();
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
        ServerLifecycleEvents.SERVER_STOPPED.register((s) -> DynamicRegistries.onSeverClosed());
    }

    private void registerArgumentTypes() {
        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation(LibReference.MOD_ID, "enum_argument"),
            LibUtil.asParentType(EnumArgument.class),
            EnumArgument.INFO);
        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation(LibReference.MOD_ID, "file_argument"),
            FileArgument.class,
            FileArgument.INFO);
        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation(LibReference.MOD_ID, "json_argument"),
            JsonArgument.class,
            JsonArgument.INFO);
        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation(LibReference.MOD_ID, "path_argument"),
            PathArgument.class,
            SingletonArgumentInfo.contextFree(PathArgument::new));
        ArgumentTypeRegistry.registerArgumentType(
            new ResourceLocation(LibReference.MOD_ID, "registry_argument"),
            LibUtil.asParentType(RegistryArgument.class),
            RegistryArgument.INFO);
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> ClientTickEvent.END.invoker().accept(mc));
    }

    private void setupBiomeModificationHook() {
        BiomeModifications.create(new ResourceLocation(LibReference.MOD_ID, "biome_updates"))
            .add(ModificationPhase.REMOVALS, s -> FeatureModificationEvent.hasEvent(s.getBiomeKey().location()), (s, m) -> {
                final FeatureModificationContext ctx = new FeatureModificationContextImpl(s, m);
                FeatureModificationEvent.global().invoker().accept(ctx);
                FeatureModificationEvent.get(s.getBiomeKey().location()).accept(ctx);
            });
    }
}
