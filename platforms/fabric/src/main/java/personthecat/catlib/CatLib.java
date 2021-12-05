package personthecat.catlib;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.Util;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.command.CatLibCommands;
import personthecat.catlib.command.CommandRegistrationContext;
import personthecat.catlib.command.CommandUtils;
import personthecat.catlib.command.DefaultLibCommands;
import personthecat.catlib.command.arguments.*;
import personthecat.catlib.config.HjsonConfigSerializer;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.player.CommonPlayerEvent;
import personthecat.catlib.event.registry.DynamicRegistries;
import personthecat.catlib.event.registry.RegistryAccessEvent;
import personthecat.catlib.event.registry.RegistryAddedEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.event.world.RegistrySet;
import personthecat.catlib.mixin.BiomeModificationContextAccessor;
import personthecat.catlib.util.LibReference;

public class CatLib implements ModInitializer {

    @Override
    public void onInitialize() {
        AutoConfig.register(LibConfig.class, HjsonConfigSerializer::new);

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

        ServerWorldEvents.LOAD.register((s, l) -> CommonWorldEvent.LOAD.invoker().accept(l));
        ServerWorldEvents.UNLOAD.register((s, l) -> CommonWorldEvent.UNLOAD.invoker().accept(l));
        ServerPlayConnectionEvents.JOIN.register((h, tx, s) -> {
            if (LibErrorContext.hasErrors()) {
                h.player.sendMessage(new TranslatableComponent("catlib.errorText.clickHere")
                    .withStyle(Style.EMPTY.withClickEvent(CommandUtils.clickToRun("/catlib errors"))), Util.NIL_UUID);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((h, tx, s) -> CommonPlayerEvent.LOGIN.invoker().accept(h.player, s));
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> CommonPlayerEvent.LOGOUT.invoker().accept(h.player, s));
    }

    @SuppressWarnings("deprecation")
    private void setupBiomeModificationHook() {
        BiomeModifications.create(new ResourceLocation(LibReference.MOD_ID, "biome_updates"))
            .add(ModificationPhase.REMOVALS, s -> true, (s, m) -> {
                final RegistrySet registries = new RegistrySet(((BiomeModificationContextAccessor) m).getRegistries());
                FeatureModificationEvent.EVENT.invoker().accept(new FeatureModificationContext(s, registries));
            });
    }
}
