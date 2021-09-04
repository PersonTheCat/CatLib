package personthecat.catlib.event.world;

import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.mixin.biome.modification.GenerationSettingsAccessor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.List;
import java.util.function.Supplier;

@OverwriteClass
@InheritMissingMembers
@SuppressWarnings("deprecation")
public class FeatureModificationContext {

    private final BiomeSelectionContext biome;
    private final GenerationSettingsAccessor generation;
    private final Registry<ConfiguredWorldCarver<?>> carvers;
    private final Registry<ConfiguredFeature<?, ?>> features;
    private final Registry<ConfiguredStructureFeature<?, ?>> structures;

    public FeatureModificationContext(final BiomeSelectionContext biome, final RegistrySet registries) {
        this.biome = biome;
        this.generation = (GenerationSettingsAccessor) biome.getBiome().getGenerationSettings();
        this.carvers = registries.getCarvers();
        this.features = registries.getFeatures();
        this.structures = registries.getStructures();
    }

    @Overwrite
    public Biome getBiome() {
        return this.biome.getBiome();
    }

    @Overwrite
    public ResourceLocation getName() {
        return this.biome.getBiomeKey().location();
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
    public List<Supplier<ConfiguredWorldCarver<?>>> getCarvers(final Carving step) {
        return this.generation.fabric_getCarvers().get(step);
    }

    @Overwrite
    public List<Supplier<ConfiguredFeature<?, ?>>> getFeatures(final Decoration step) {
        return this.generation.fabric_getFeatures().get(step.ordinal());
    }

    @Overwrite
    public List<Supplier<ConfiguredStructureFeature<?, ?>>> getStructures() {
        return this.generation.fabric_getStructureFeatures();
    }
}

