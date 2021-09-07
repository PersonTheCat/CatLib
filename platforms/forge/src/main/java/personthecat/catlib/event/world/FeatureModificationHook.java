package personthecat.catlib.event.world;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import org.jetbrains.annotations.ApiStatus;
import personthecat.catlib.mixin.BiomeGenerationSettingsAccessor;
import personthecat.catlib.mixin.BiomeGenerationSettingsBuilderAccessor;

import java.util.Set;
import java.util.function.Consumer;

public class FeatureModificationHook {

    @ApiStatus.Internal
    @SuppressWarnings("ConstantConditions")
    public static void onRegistryAccess(final RegistryAccess holder) {
        if (!FeatureModificationEvent.EVENT.isEmpty()) {
            final Consumer<FeatureModificationContext> event = FeatureModificationEvent.EVENT.invoker();
            final RegistrySet registries = new RegistrySet(holder);
            final Set<ResourceLocation> modifiedBiomes = ((RegistryAccessTracker) holder).getModifiedBiomes();

            holder.registryOrThrow(Registry.BIOME_REGISTRY).forEach(biome -> {
                if (!modifiedBiomes.add(biome.getRegistryName())) {
                    return; // Don't double modify;
                }
                final BiomeGenerationSettingsBuilder builder = new BiomeGenerationSettingsBuilder(biome.getGenerationSettings());
                event.accept(new FeatureModificationContext(biome, builder, registries));

                final BiomeGenerationSettingsAccessor settingsAccessor = (BiomeGenerationSettingsAccessor) biome.getGenerationSettings();
                final BiomeGenerationSettingsBuilderAccessor builderAccessor = (BiomeGenerationSettingsBuilderAccessor) builder;
                settingsAccessor.setCarvers(builderAccessor.getCarvers());
                settingsAccessor.setFeatures(builderAccessor.getFeatures());
                settingsAccessor.setStructureStarts(builder.getStructures());
            });
        }
    }
}
