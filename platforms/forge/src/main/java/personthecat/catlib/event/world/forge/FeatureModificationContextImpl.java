package personthecat.catlib.event.world.forge;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.registry.RegistrySet;

import java.util.List;
import java.util.function.Predicate;

public class FeatureModificationContextImpl extends FeatureModificationContext {

    private final Biome biome;
    private final BiomeGenerationSettingsBuilder builder;
    private final Registry<ConfiguredWorldCarver<?>> carvers;
    private final Registry<PlacedFeature> features;
    private final Registry<ConfiguredStructureFeature<?, ?>> structures;
    private final RegistryAccess registries;

    public FeatureModificationContextImpl(Biome biome, BiomeGenerationSettingsBuilder builder, RegistrySet registries) {
        this.biome = biome;
        this.builder = builder;
        this.carvers = registries.getCarvers();
        this.features = registries.getFeatures();
        this.structures = registries.getStructures();
        this.registries = registries.getRegistries();
    }

    @Override
    public Biome getBiome() {
        return this.biome;
    }

    @Override
    public ResourceLocation getName() {
        return this.biome.getRegistryName();
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
    public List<Holder<ConfiguredWorldCarver<?>>> getCarvers(final Carving step) {
        return this.builder.getCarvers(step);
    }

    @Override
    public HolderSet<PlacedFeature> getFeatures(final Decoration step) {
        return HolderSet.direct(this.getFeaturesInternal(step));
    }

    private List<Holder<PlacedFeature>> getFeaturesInternal(final Decoration step) {
        return this.builder.getFeatures(step);
    }

    @Override
    public boolean removeCarver(final Carving step, final ResourceLocation id) {
        final ConfiguredWorldCarver<?> carver = this.getCarverRegistry().get(id);
        if (carver == null) return false;
        return this.getCarvers(step).removeIf(holder -> carver.equals(holder.value()));
    }

    @Override
    public boolean removeCarver(final Carving step, final Predicate<ConfiguredWorldCarver<?>> predicate) {
        return this.getCarvers(step).removeIf(holder -> predicate.test(holder.value()));
    }

    @Override
    public boolean removeFeature(final Decoration step, final ResourceLocation id) {
        final PlacedFeature feature = this.getFeatureRegistry().get(id);
        if (feature == null) return false;
        return this.getFeaturesInternal(step).removeIf(holder -> feature.equals(holder.value()));
    }

    @Override
    public boolean removeFeature(final Decoration step, final Predicate<PlacedFeature> predicate) {
        return this.getFeaturesInternal(step).removeIf(holder -> predicate.test(holder.value()));
    }

    @Override
    public void addCarver(final Carving step, final ConfiguredWorldCarver<?> carver) {
        this.builder.addCarver(step, Holder.direct(carver));
    }

    @Override
    public void addFeature(final Decoration step, final PlacedFeature feature) {
        this.builder.addFeature(step, Holder.direct(feature));
    }
}
