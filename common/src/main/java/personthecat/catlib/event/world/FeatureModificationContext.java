package personthecat.catlib.event.world;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.TemperatureModifier;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class FeatureModificationContext {

    public abstract Holder<Biome> getBiome();

    public abstract ResourceLocation getName();

    public abstract Registry<ConfiguredWorldCarver<?>> getCarverRegistry();

    public abstract Registry<PlacedFeature> getFeatureRegistry();

    public abstract RegistryAccess getRegistryAccess();

    public abstract boolean hasPrecipitation();

    public abstract float getTemperature();

    public abstract TemperatureModifier getTemperatureModifier();

    public abstract float getDownfall();

    public abstract void setHasPrecipitation(final boolean hasPrecipitation);

    public abstract void setTemperature(final float temperature);

    public abstract void setTemperatureModifier(final TemperatureModifier modifier);

    public abstract void setDownfall(final float downfall);

    public abstract int getFogColor();

    public abstract int getWaterColor();

    public abstract int getWaterFogColor();

    public abstract int getSkyColor();

    public abstract GrassColorModifier getGrassColorModifier();

    public abstract Optional<Integer> getFoliageColorOverride();

    public abstract Optional<Integer> getGrassColorOverride();

    public abstract Optional<AmbientParticleSettings> getAmbientParticleSettings();

    public abstract Optional<Holder<SoundEvent>> getAmbientLoopSound();

    public abstract Optional<AmbientMoodSettings> getAmbientMoodSettings();

    public abstract Optional<AmbientAdditionsSettings> getAmbientAdditions();

    public abstract Optional<Music> getBackgroundMusic();

    public abstract void setFogColor(final int color);

    public abstract void setWaterColor(final int color);

    public abstract void setWaterFogColor(final int color);

    public abstract void setSkyColor(final int color);

    public abstract void setGrassColorModifier(final GrassColorModifier modifier);

    public abstract void setFoliageColorOverride(final @Nullable Integer override);

    public abstract void setGrassColorOverride(final @Nullable Integer override);

    public abstract void setAmbientParticleSettings(final @Nullable AmbientParticleSettings settings);

    public abstract void setAmbientLoopSound(final @Nullable Holder<SoundEvent> event);

    public abstract void setAmbientMoodSound(final @Nullable AmbientMoodSettings settings);

    public abstract void setAmbientAdditionsSound(final @Nullable AmbientAdditionsSettings settings);

    public abstract void setBackgroundMusic(final @Nullable Music music);

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

    public abstract Collection<MobCategory> getSpawnCategories();

    public abstract Collection<EntityType<?>> getEntityTypes();

    public abstract float getCreatureGenerationProbability();

    public abstract void addSpawn(final MobCategory category, final SpawnerData data);

    public void addSpawn(final MobCategory category, final EntityType<?> type, int weight, int minCount, int maxCount) {
        this.addSpawn(category, new SpawnerData(type, weight, minCount, maxCount));
    }

    public abstract void setSpawnCost(final EntityType<?> type, final double mass, final double gravityLimit);

    public abstract void removeSpawns(final MobCategory category);

    public abstract void removeSpawn(final EntityType<?> type);

    public abstract void removeSpawnCost(final EntityType<?> type);
}
