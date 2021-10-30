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
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
    public Registry<ConfiguredFeature<?, ?>> getFeatureRegistry() {
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
    public List<Supplier<ConfiguredWorldCarver<?>>> getCarvers(final Carving step) {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public List<Supplier<ConfiguredFeature<?, ?>>> getFeatures(final Decoration step) {
        throw new MissingOverrideException();
    }

    @PlatformMustOverwrite
    public List<Supplier<ConfiguredStructureFeature<?, ?>>> getStructures() {
        throw new MissingOverrideException();
    }

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

    public boolean removeCarver(final Carving step, final ResourceLocation id) {
        final ConfiguredWorldCarver<?> carver = this.getCarverRegistry().get(id);
        if (carver == null) return false;
        return this.getCarvers(step).removeIf(supplier -> carver.equals(supplier.get()));
    }

    public boolean removeCarver(final Carving step, final Predicate<ConfiguredWorldCarver<?>> predicate) {
        return this.getCarvers(step).removeIf(supplier -> predicate.test(supplier.get()));
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

    public boolean removeFeature(final Decoration step, final ResourceLocation id) {
        final ConfiguredFeature<?, ?> feature = this.getFeatureRegistry().get(id);
        if (feature == null) return false;
        return this.getFeatures(step).removeIf(supplier -> feature.equals(supplier.get()));
    }

    public boolean removeFeature(final Decoration step, final Predicate<ConfiguredFeature<?, ?>> predicate) {
        return this.getFeatures(step).removeIf(supplier -> predicate.test(supplier.get()));
    }

    public boolean removeStructure(final ResourceLocation id) {
        final ConfiguredStructureFeature<?, ?> structure = this.getStructureRegistry().get(id);
        if (structure == null) return false;
        return this.getStructures().removeIf(supplier -> structure.equals(supplier.get()));
    }

    public boolean removeStructure(final Predicate<ConfiguredStructureFeature<?, ?>> predicate) {
        return this.getStructures().removeIf(supplier -> predicate.test(supplier.get()));
    }

    public void addCarver(final Carving step, final ConfiguredWorldCarver<?> carver) {
        this.getCarvers(step).add(() -> carver);
    }

    public void addFeature(final Decoration step, final ConfiguredFeature<?, ?> feature) {
        this.getFeatures(step).add(() -> feature);
    }

    public void addStructure(final ConfiguredStructureFeature<?, ?> structure) {
        this.getStructures().add(() -> structure);
    }
}
