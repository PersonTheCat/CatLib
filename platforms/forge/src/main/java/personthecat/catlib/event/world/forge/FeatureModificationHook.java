package personthecat.catlib.event.world.forge;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import org.jetbrains.annotations.ApiStatus;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.event.world.FeatureModificationEvent;
import personthecat.catlib.event.world.forge.FeatureModificationContextImpl;
import personthecat.catlib.mixin.forge.BiomeGenerationSettingsAccessor;
import personthecat.catlib.mixin.forge.BiomeGenerationSettingsBuilderAccessor;
import personthecat.catlib.mixin.DirectHolderSetAccessor;
import personthecat.catlib.mixin.NamedHolderSetAccessor;
import personthecat.catlib.registry.RegistrySet;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

@Log4j2
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
                updateCarvers(settingsAccessor.getCarvers(), builderAccessor.getCarvers(), biome);
                updateFeatures(settingsAccessor.getFeatures(), builderAccessor.getFeatures(), biome);
            });

            if (!modifiedBiomes.isEmpty()) {
                log.info("Successfully updated {} biomes using modification hook!", modifiedBiomes.size());
            }
        }
    }

    private static void updateCarvers(
            final Map<GenerationStep.Carving, HolderSet<ConfiguredWorldCarver<?>>> carvers,
            final Map<GenerationStep.Carving, List<Holder<ConfiguredWorldCarver<?>>>> updated,
            final Biome biome) {
        for (final Map.Entry<GenerationStep.Carving, HolderSet<ConfiguredWorldCarver<?>>> entry : carvers.entrySet()) {
            updateHolderSet(entry.getValue(), updated.get(entry.getKey()), biome);
        }
    }

    private static void updateFeatures(
            final List<HolderSet<PlacedFeature>> features,
            final List<List<Holder<PlacedFeature>>> updated,
            final Biome biome) {
        for (int i = 0; i < Math.min(features.size(), updated.size()); i++) {
            updateHolderSet(features.get(i), updated.get(i), biome);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void updateHolderSet(
            final HolderSet<T> holderSet, final List<Holder<T>> contents, final Biome biome) {
        if (holderSet instanceof DirectHolderSetAccessor) {
            final DirectHolderSetAccessor<T> accessor = (DirectHolderSetAccessor<T>) holderSet;
            accessor.setContents(contents);
            accessor.setContentsSet(null);
        } else if (holderSet instanceof NamedHolderSetAccessor) {
            final NamedHolderSetAccessor<T> accessor = (NamedHolderSetAccessor<T>) holderSet;
            accessor.invokeBind(contents);
        } else {
            log.error("Cannot apply transforms to {}. Unknown holder set in place.", biome.getRegistryName());
        }
    }
}
