package personthecat.catlib.event.world;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import org.jetbrains.annotations.ApiStatus;
import personthecat.catlib.event.world.forge.FeatureModificationContextImpl;
import personthecat.catlib.mixin.BiomeGenerationSettingsAccessor;
import personthecat.catlib.mixin.BiomeGenerationSettingsBuilderAccessor;
import personthecat.catlib.registry.RegistrySet;

import java.util.*;
import java.util.function.Consumer;

public class FeatureModificationHook {

    private static final Map<RegistryAccess, Set<ResourceLocation>> MODIFIED_BIOMES =
        Collections.synchronizedMap(new WeakHashMap<>());

    @ApiStatus.Internal
    @SuppressWarnings("ConstantConditions")
    public static void onRegistryAccess(final RegistryAccess holder) {
        if (!FeatureModificationEvent.EVENT.isEmpty()) {
            final Consumer<FeatureModificationContext> event = FeatureModificationEvent.EVENT.invoker();
            final RegistrySet registries = new RegistrySet(holder);

            final Set<ResourceLocation> modifiedBiomes = MODIFIED_BIOMES
                .computeIfAbsent(holder, h -> Collections.synchronizedSet(new HashSet<>()));

            holder.registryOrThrow(Registry.BIOME_REGISTRY).forEach(biome -> {
                if (!modifiedBiomes.add(biome.getRegistryName())) {
                    return; // Don't double modify;
                }
                final BiomeGenerationSettingsBuilder builder = new BiomeGenerationSettingsBuilder(biome.getGenerationSettings());
                event.accept(new FeatureModificationContextImpl(biome, builder, registries));

                final BiomeGenerationSettingsAccessor settingsAccessor = (BiomeGenerationSettingsAccessor) biome.getGenerationSettings();
                final BiomeGenerationSettingsBuilderAccessor builderAccessor = (BiomeGenerationSettingsBuilderAccessor) builder;
                settingsAccessor.setCarvers(builderAccessor.getCarvers());
                settingsAccessor.setFeatures(builderAccessor.getFeatures());
            });
        }
    }
}
