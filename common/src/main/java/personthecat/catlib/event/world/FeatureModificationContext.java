package personthecat.catlib.event.world;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

import java.util.function.Predicate;

@OverwriteTarget(required = true)
public class FeatureModificationContext {

    @PlatformMustOverwrite
    public Biome getBiome() {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public ResourceLocation getName() {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public Registry<ConfiguredWorldCarver<?>> getCarverRegistry() {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public Registry<PlacedFeature> getFeatureRegistry() {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public Registry<ConfiguredStructureFeature<?, ?>> getStructureRegistry() {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public RegistryAccess getRegistryAccess() {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers(final Carving step) {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public HolderSet<PlacedFeature> getFeatures(final Decoration step) {
        throw new MissingOverrideException();
    }

//    @PlatformMustOverwrite
//    public List<Supplier<ConfiguredStructureFeature<?, ?>>> getStructures() {
//        throw new MissingOverrideException();
//    }

    public boolean removeCarver(final ResourceLocation id) {
        boolean anyRemoved = false;
        for (final Carving step : Carving.values()) {
            anyRemoved |= this.removeCarver(step, id);
        }
        return anyRemoved;
    }

    public boolean removeCarver(final Predicate<ConfiguredWorldCarver<?>> predicate) {
        boolean anyRemoved = false;
        for (final Carving step : Carving.values()) {
            anyRemoved |= this.removeCarver(step, predicate);
        }
        return anyRemoved;
    }

    @PlatformMustOverwrite
    public boolean removeCarver(final Carving step, final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public boolean removeCarver(final Carving step, final Predicate<ConfiguredWorldCarver<?>> predicate) {
        throw new MissingOverrideException();
    }

    public boolean removeFeature(final ResourceLocation id) {
        boolean anyRemoved = false;
        for (final Decoration step : Decoration.values()) {
            anyRemoved |= this.removeFeature(step, id);
        }
        return anyRemoved;
    }

    public boolean removeFeature(final Predicate<ConfiguredFeature<?, ?>> predicate) {
        boolean anyRemoved = false;
        for (final Decoration step : Decoration.values()) {
            anyRemoved |= this.removeFeature(step, predicate);
        }
        return anyRemoved;
    }

    @PlatformMustOverwrite
    public boolean removeFeature(final Decoration step, final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public boolean removeFeature(final Decoration step, final Predicate<ConfiguredFeature<?, ?>> predicate) {
        throw new MissingOverrideException();
    }

//    public boolean removeStructure(final ResourceLocation id) {
//        final ConfiguredStructureFeature<?, ?> structure = this.getStructureRegistry().get(id);
//        if (structure == null) return false;
//        return this.getStructures().removeIf(supplier -> structure.equals(supplier.get()));
//    }
//
//    public boolean removeStructure(final Predicate<ConfiguredStructureFeature<?, ?>> predicate) {
//        return this.getStructures().removeIf(supplier -> predicate.test(supplier.get()));
//    }

    @PlatformMustOverwrite
    public void addCarver(final Carving step, final ConfiguredWorldCarver<?> carver) {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public void addFeature(final Decoration step, final ConfiguredFeature<?, ?> feature) {
        throw new MissingOverrideException();
    }

//    public void addStructure(final ConfiguredStructureFeature<?, ?> structure) {
//        this.getStructures().add(() -> structure);
//    }
}
