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
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ClientFeatureContext extends FeatureModificationContext {
    private final FeatureModificationContext wrapped;

    public ClientFeatureContext(final FeatureModificationContext wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean isServerSide() {
        return false;
    }

    @Override
    public Holder<Biome> getBiome() {
        return this.wrapped.getBiome();
    }

    @Override
    public ResourceLocation getName() {
        return this.wrapped.getName();
    }

    @Override
    public Registry<ConfiguredWorldCarver<?>> getCarverRegistry() {
        return this.wrapped.getCarverRegistry();
    }

    @Override
    public Registry<PlacedFeature> getFeatureRegistry() {
        return this.wrapped.getFeatureRegistry();
    }

    @Override
    public RegistryAccess getRegistryAccess() {
        return this.wrapped.getRegistryAccess();
    }

    @Override
    public boolean hasPrecipitation() {
        return this.wrapped.hasPrecipitation();
    }

    @Override
    public float getTemperature() {
        return this.wrapped.getTemperature();
    }

    @Override
    public TemperatureModifier getTemperatureModifier() {
        return this.wrapped.getTemperatureModifier();
    }

    @Override
    public float getDownfall() {
        return this.wrapped.getDownfall();
    }

    @Override
    public void setHasPrecipitation(final boolean hasPrecipitation) {
        this.wrapped.setHasPrecipitation(hasPrecipitation);
    }

    @Override
    public void setTemperature(final float temperature) {
        this.wrapped.setTemperature(temperature);
    }

    @Override
    public void setTemperatureModifier(final TemperatureModifier modifier) {
        this.wrapped.setTemperatureModifier(modifier);
    }

    @Override
    public void setDownfall(final float downfall) {
        this.wrapped.setDownfall(downfall);
    }

    @Override
    public int getFogColor() {
        return this.wrapped.getFogColor();
    }

    @Override
    public int getWaterColor() {
        return this.wrapped.getWaterColor();
    }

    @Override
    public int getWaterFogColor() {
        return this.wrapped.getWaterFogColor();
    }

    @Override
    public int getSkyColor() {
        return this.wrapped.getSkyColor();
    }

    @Override
    public GrassColorModifier getGrassColorModifier() {
        return this.wrapped.getGrassColorModifier();
    }

    @Override
    public Optional<Integer> getFoliageColorOverride() {
        return this.wrapped.getFoliageColorOverride();
    }

    @Override
    public Optional<Integer> getGrassColorOverride() {
        return this.wrapped.getGrassColorOverride();
    }

    @Override
    public Optional<AmbientParticleSettings> getAmbientParticleSettings() {
        return this.wrapped.getAmbientParticleSettings();
    }

    @Override
    public Optional<Holder<SoundEvent>> getAmbientLoopSound() {
        return this.wrapped.getAmbientLoopSound();
    }

    @Override
    public Optional<AmbientMoodSettings> getAmbientMoodSettings() {
        return this.wrapped.getAmbientMoodSettings();
    }

    @Override
    public Optional<AmbientAdditionsSettings> getAmbientAdditions() {
        return this.wrapped.getAmbientAdditions();
    }

    @Override
    public Optional<Music> getBackgroundMusic() {
        return this.wrapped.getBackgroundMusic();
    }

    @Override
    public void setFogColor(final int color) {
        this.wrapped.setFogColor(color);
    }

    @Override
    public void setWaterColor(final int color) {
        this.wrapped.setWaterColor(color);
    }

    @Override
    public void setWaterFogColor(final int color) {
        this.wrapped.setWaterFogColor(color);
    }

    @Override
    public void setSkyColor(final int color) {
        this.wrapped.setSkyColor(color);
    }

    @Override
    public void setGrassColorModifier(final GrassColorModifier modifier) {
        this.wrapped.setGrassColorModifier(modifier);
    }

    @Override
    public void setFoliageColorOverride(final @Nullable Integer override) {
        this.wrapped.setFoliageColorOverride(override);
    }

    @Override
    public void setGrassColorOverride(final @Nullable Integer override) {
        this.wrapped.setGrassColorOverride(override);
    }

    @Override
    public void setAmbientParticleSettings(final @Nullable AmbientParticleSettings settings) {
        this.wrapped.setAmbientParticleSettings(settings);
    }

    @Override
    public void setAmbientLoopSound(final @Nullable Holder<SoundEvent> event) {
        this.wrapped.setAmbientLoopSound(event);
    }

    @Override
    public void setAmbientMoodSound(final @Nullable AmbientMoodSettings settings) {
        this.wrapped.setAmbientMoodSound(settings);
    }

    @Override
    public void setAmbientAdditionsSound(final @Nullable AmbientAdditionsSettings settings) {
        this.wrapped.setAmbientAdditionsSound(settings);
    }

    @Override
    public void setBackgroundMusic(final @Nullable Music music) {
        this.wrapped.setBackgroundMusic(music);
    }

    @Override
    public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers(final Carving step) {
        return List.of();
    }

    @Override
    public Iterable<Holder<PlacedFeature>> getFeatures(final Decoration step) {
        return List.of();
    }

    @Override
    public boolean removeCarver(final Carving step, final ResourceLocation id) {
        return false;
    }

    @Override
    public boolean removeCarver(final Carving step, final Predicate<Holder<ConfiguredWorldCarver<?>>> predicate) {
        return false;
    }

    @Override
    public boolean removeFeature(final Decoration step, final ResourceLocation id) {
        return false;
    }

    @Override
    public boolean removeFeature(final Decoration step, final Predicate<Holder<PlacedFeature>> predicate) {
        return false;
    }

    @Override
    public void addCarver(final Carving step, final Holder<ConfiguredWorldCarver<?>> carver) {}

    @Override
    public void addFeature(final Decoration step, final Holder<PlacedFeature> feature) {}

    @Override
    public Collection<MobCategory> getSpawnCategories() {
        return List.of();
    }

    @Override
    public Collection<EntityType<?>> getEntityTypes() {
        return List.of();
    }

    @Override
    public float getCreatureGenerationProbability() {
        return 0;
    }

    @Override
    public void setCreatureSpawnProbability(final float probability) {}

    @Override
    public void addSpawn(final MobCategory category, final SpawnerData data) {}

    @Override
    public void setSpawnCost(final EntityType<?> type, final double mass, final double gravityLimit) {}

    @Override
    public void removeSpawns(final MobCategory category) {}

    @Override
    public void removeSpawn(final EntityType<?> type) {}

    @Override
    public void removeSpawnCost(final EntityType<?> type) {}
}
