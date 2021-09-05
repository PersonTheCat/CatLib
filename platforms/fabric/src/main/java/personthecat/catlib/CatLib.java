package personthecat.catlib;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.command.CommandRegistrationContext;
import personthecat.catlib.command.DefaultLibCommands;
import personthecat.catlib.command.arguments.FileArgument;
import personthecat.catlib.command.arguments.HjsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.config.HjsonConfigSerializer;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.event.world.RegistrySet;
import personthecat.catlib.mixin.BiomeModificationContextAccessor;
import personthecat.catlib.util.LibReference;

public class CatLib implements ModInitializer {

    @Override
    public void onInitialize() {
        AutoConfig.register(LibConfig.class, HjsonConfigSerializer::new);

        FileArgument.register();
        HjsonArgument.register();
        PathArgument.register();

        this.setupBiomeModificationHook();

        if (LibConfig.ENABLE_GLOBAL_LIB_COMMANDS.get()) {
            CommandRegistrationContext.forMod(LibReference.MOD_DESCRIPTOR)
                .addAllCommands(DefaultLibCommands.createAll(LibReference.MOD_DESCRIPTOR, true))
                .registerAll();
        }
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
