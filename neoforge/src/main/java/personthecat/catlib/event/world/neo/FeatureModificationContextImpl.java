package personthecat.catlib.event.world.neo;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import personthecat.catlib.event.world.FeatureModificationContext;

import java.util.List;
import java.util.function.Predicate;

public class FeatureModificationContextImpl extends FeatureModificationContext {
    private final Biome biome;
    private final ResourceLocation name;
    private final Builder builder;
    private final Registry<ConfiguredWorldCarver<?>> carvers;
    private final Registry<PlacedFeature> features;
    private final Registry<Structure> structures;
    private final RegistryAccess registries;

    public FeatureModificationContextImpl(
            Biome biome, ResourceLocation name, RegistryAccess registries, Builder builder) {
        this.biome = biome;
        this.name = name;
        this.builder = builder;
        this.carvers = registries.registryOrThrow(Registries.CONFIGURED_CARVER);
        this.features = registries.registryOrThrow(Registries.PLACED_FEATURE);
        this.structures = registries.registryOrThrow(Registries.STRUCTURE);
        this.registries = registries;
    }

    @Override
    public Biome getBiome() {
        return this.biome;
    }

    @Override
    public ResourceLocation getName() {
        return this.name;
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
    public Registry<Structure> getStructureRegistry() {
        return this.structures;
    }

    @Override
    public RegistryAccess getRegistryAccess() {
        return this.registries;
    }

    @Override
    public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers(Carving step) {
        return this.builder.getGenerationSettings().getCarvers(step);
    }

    @Override
    public List<Holder<PlacedFeature>> getFeatures(Decoration step) {
        return this.builder.getGenerationSettings().getFeatures(step);
    }

    @Override
    public boolean removeCarver(Carving step, ResourceLocation id) {
        return this.builder.getGenerationSettings().getCarvers(step)
            .removeIf(holder -> id.equals(keyOf(this.carvers, holder)));
    }

    @Override
    public boolean removeCarver(Carving step, Predicate<ConfiguredWorldCarver<?>> predicate) {
        return this.builder.getGenerationSettings().getCarvers(step)
            .removeIf(holder -> predicate.test(holder.value()));
    }

    @Override
    public boolean removeFeature(Decoration step, ResourceLocation id) {
        return this.builder.getGenerationSettings().getFeatures(step)
            .removeIf(holder -> id.equals(keyOf(this.features, holder)));
    }

    @Override
    public boolean removeFeature(Decoration step, Predicate<PlacedFeature> predicate) {
        return this.builder.getGenerationSettings().getFeatures(step)
            .removeIf(holder -> predicate.test(holder.value()));
    }

    @Override
    public void addCarver(Carving step, ConfiguredWorldCarver<?> carver) {
        this.builder.getGenerationSettings().addCarver(step, Holder.direct(carver));
    }

    @Override
    public void addFeature(Decoration step, PlacedFeature feature) {
        this.builder.getGenerationSettings().addFeature(step, Holder.direct(feature));
    }

    private static <T> ResourceLocation keyOf(final Registry<T> registry, final Holder<T> holder) {
        return holder.unwrapKey()
            .map(ResourceKey::location) // value must be present if no location
            .orElseGet(() -> registry.getKey(holder.value()));
    }
}
