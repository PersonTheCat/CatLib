package personthecat.catlib.event.world;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.List;
import java.util.function.Supplier;

@OverwriteClass
@InheritMissingMembers
public class FeatureModificationContext {

    private final Biome biome;
    private final BiomeGenerationSettingsBuilder builder;
    private final Registry<ConfiguredWorldCarver<?>> carvers;
    private final Registry<ConfiguredFeature<?, ?>> features;
    private final Registry<ConfiguredStructureFeature<?, ?>> structures;
    private final RegistryAccess registries;

    public FeatureModificationContext(Biome biome, BiomeGenerationSettingsBuilder builder, RegistrySet registries) {
        this.biome = biome;
        this.builder = builder;
        this.carvers = registries.getCarvers();
        this.features = registries.getFeatures();
        this.structures = registries.getStructures();
        this.registries = registries.getRegistries();
    }

    @Overwrite
    public Biome getBiome() {
        return this.biome;
    }

    @Overwrite
    public ResourceLocation getName() {
        return this.biome.getRegistryName();
    }

    @Overwrite
    public Registry<ConfiguredWorldCarver<?>> getCarverRegistry() {
        return this.carvers;
    }

    @Overwrite
    public Registry<ConfiguredFeature<?, ?>> getFeatureRegistry() {
        return this.features;
    }

    @Overwrite
    public Registry<ConfiguredStructureFeature<?, ?>> getStructureRegistry() {
        return this.structures;
    }

    @Overwrite
    public RegistryAccess getRegistryAccess() {
        return this.registries;
    }

    @Overwrite
    public List<Supplier<ConfiguredWorldCarver<?>>> getCarvers(final Carving step) {
        return this.builder.getCarvers(step);
    }

    @Overwrite
    public List<Supplier<ConfiguredFeature<?, ?>>> getFeatures(final Decoration step) {
        return this.builder.getFeatures(step);
    }

    @Overwrite
    public List<Supplier<ConfiguredStructureFeature<?, ?>>> getStructures() {
        return this.builder.getStructures();
    }
}
