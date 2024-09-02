package personthecat.catlib.event.world;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.function.Predicate;

public abstract class FeatureModificationContext {

    public abstract Holder<Biome> getBiome();

    public abstract ResourceLocation getName();

    public abstract Registry<ConfiguredWorldCarver<?>> getCarverRegistry();

    public abstract Registry<PlacedFeature> getFeatureRegistry();

    public abstract Registry<Structure> getStructureRegistry();

    public abstract RegistryAccess getRegistryAccess();

    public abstract Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers(final Carving step);

    public abstract Iterable<Holder<PlacedFeature>> getFeatures(final Decoration step);

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

    public abstract boolean removeCarver(final Carving step, final ResourceLocation id);

    public abstract boolean removeCarver(final Carving step, final Predicate<ConfiguredWorldCarver<?>> predicate);

    public boolean removeFeature(final ResourceLocation id) {
        boolean anyRemoved = false;
        for (final Decoration step : Decoration.values()) {
            anyRemoved |= this.removeFeature(step, id);
        }
        return anyRemoved;
    }

    public boolean removeFeature(final Predicate<PlacedFeature> predicate) {
        boolean anyRemoved = false;
        for (final Decoration step : Decoration.values()) {
            anyRemoved |= this.removeFeature(step, predicate);
        }
        return anyRemoved;
    }

    public abstract boolean removeFeature(final Decoration step, final ResourceLocation id);

    public abstract boolean removeFeature(final Decoration step, final Predicate<PlacedFeature> predicate);

    public abstract void addCarver(final Carving step, final ConfiguredWorldCarver<?> carver);

    public abstract void addFeature(final Decoration step, final PlacedFeature feature);

}
