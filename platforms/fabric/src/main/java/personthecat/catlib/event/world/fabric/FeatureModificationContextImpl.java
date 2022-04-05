package personthecat.catlib.event.world.fabric;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.registry.RegistrySet;
import personthecat.catlib.mixin.BiomeModificationContextAccessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class FeatureModificationContextImpl extends FeatureModificationContext {

    private final BiomeSelectionContext biome;
    private final BiomeModificationContext.GenerationSettingsContext modifications;
    private final BiomeGenerationSettings generation;
    private final Registry<ConfiguredWorldCarver<?>> carvers;
    private final Registry<PlacedFeature> features;
    private final Registry<ConfiguredStructureFeature<?, ?>> structures;
    private final RegistryAccess registries;

    public FeatureModificationContextImpl(final BiomeSelectionContext biome, final BiomeModificationContext ctx) {
        final RegistrySet registries = new RegistrySet(((BiomeModificationContextAccessor) ctx).getRegistries());
        this.biome = biome;
        this.modifications = ctx.getGenerationSettings();
        this.generation = biome.getBiome().getGenerationSettings();
        this.carvers = registries.getCarvers();
        this.features = registries.getFeatures();
        this.structures = registries.getStructures();
        this.registries = registries.getRegistries();
    }

    @Override
    public Biome getBiome() {
        return this.biome.getBiome();
    }

    @Override
    public ResourceLocation getName() {
        return this.biome.getBiomeKey().location();
    }

    @Override
    public Registry<ConfiguredWorldCarver<?>> getCarverRegistry() {
        return this.carvers;
    }

    @Override
    public Registry<PlacedFeature> getFeatureRegistry() {
        return this.features;
    }

    @Override
    public Registry<ConfiguredStructureFeature<?, ?>> getStructureRegistry() {
        return this.structures;
    }

    @Override
    public RegistryAccess getRegistryAccess() {
        return this.registries;
    }

    @Override
    public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers(final Carving step) {
        return this.generation.getCarvers(step);
    }

    @Override
    public HolderSet<PlacedFeature> getFeatures(final Decoration step) {
        final List<HolderSet<PlacedFeature>> features = this.generation.features();
        while (features.size() <= step.ordinal()) {
            features.add(HolderSet.direct(Collections.emptyList()));
        }
        return this.generation.features().get(step.ordinal());
    }

    @Override
    public boolean removeCarver(final Carving step, final ResourceLocation id) {
        return this.modifications.removeCarver(step, ResourceKey.create(Registry.CONFIGURED_CARVER_REGISTRY, id));
    }

    @Override
    public boolean removeCarver(final Carving step, final Predicate<ConfiguredWorldCarver<?>> predicate) {
        for (final Map.Entry<ResourceKey<ConfiguredWorldCarver<?>>, ConfiguredWorldCarver<?>> entry : this.carvers.entrySet()) {
            if (predicate.test(entry.getValue())) {
                return this.modifications.removeCarver(step, entry.getKey());
            }
        }
        return false;
    }

    @Override
    public boolean removeFeature(final Decoration step, final ResourceLocation id) {
        return this.modifications.removeFeature(step, ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, id));
    }

    @Override
    public boolean removeFeature(final Decoration step, final Predicate<PlacedFeature> predicate) {
         for (final Map.Entry<ResourceKey<PlacedFeature>, PlacedFeature> entry : this.features.entrySet()) {
             if (predicate.test(entry.getValue())) {
                 return this.modifications.removeFeature(step, entry.getKey());
             }
         }
         return false;
    }

    @Override
    public void addCarver(final Carving step, final ConfiguredWorldCarver<?> carver) {
        final ResourceKey<ConfiguredWorldCarver<?>> key = this.carvers.getResourceKey(carver)
            .orElseThrow(() -> new NullPointerException("Carvers must be registered prior to feature modification"));
        this.modifications.addCarver(step, key);
    }

    @Override
    public void addFeature(final Decoration step, final PlacedFeature feature) {
        final ResourceKey<PlacedFeature> key = this.features.getResourceKey(feature)
            .orElseThrow(() -> new NullPointerException("Features must be registered prior to feature modification"));
        this.modifications.addFeature(step, key);
    }
}

