package personthecat.catlib.event.world.neo;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.mixin.neo.BiomeSpecialEffectsBuilderAccessor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class FeatureModificationContextImpl extends FeatureModificationContext {
    private final Holder<Biome> biome;
    private final ResourceKey<Biome> key;
    private final Builder builder;
    private final Registry<ConfiguredWorldCarver<?>> carvers;
    private final Registry<PlacedFeature> features;
    private final RegistryAccess registries;

    public FeatureModificationContextImpl(
            Holder<Biome> biome, ResourceKey<Biome> key, RegistryAccess registries, Builder builder) {
        this.biome = biome;
        this.key = key;
        this.builder = builder;
        this.carvers = registries.registryOrThrow(Registries.CONFIGURED_CARVER);
        this.features = registries.registryOrThrow(Registries.PLACED_FEATURE);
        this.registries = registries;
    }

    @Override
    public Holder<Biome> getBiome() {
        return this.biome;
    }

    @Override
    public ResourceKey<Biome> getKey() {
        return this.key;
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
    public RegistryAccess getRegistryAccess() {
        return this.registries;
    }

    @Override
    public boolean hasPrecipitation() {
        return this.builder.getClimateSettings().hasPrecipitation();
    }

    @Override
    public float getTemperature() {
        return this.builder.getClimateSettings().getTemperature();
    }

    @Override
    public TemperatureModifier getTemperatureModifier() {
        return this.builder.getClimateSettings().getTemperatureModifier();
    }

    @Override
    public float getDownfall() {
        return this.builder.getClimateSettings().getDownfall();
    }

    @Override
    public void setHasPrecipitation(final boolean hasPrecipitation) {
        this.builder.getClimateSettings().setHasPrecipitation(hasPrecipitation);
        this.clientFeaturesModified();
    }

    @Override
    public void setTemperature(final float temperature) {
        this.builder.getClimateSettings().setTemperature(temperature);
        this.clientFeaturesModified();
    }

    @Override
    public void setTemperatureModifier(final TemperatureModifier modifier) {
        this.builder.getClimateSettings().setTemperatureModifier(modifier);
        this.clientFeaturesModified();
    }

    @Override
    public void setDownfall(final float downfall) {
        this.builder.getClimateSettings().setDownfall(downfall);
        this.clientFeaturesModified();
    }

    @Override
    public int getFogColor() {
        return this.builder.getSpecialEffects().getFogColor();
    }

    @Override
    public int getWaterColor() {
        return this.builder.getSpecialEffects().waterColor();
    }

    @Override
    public int getWaterFogColor() {
        return this.builder.getSpecialEffects().getWaterFogColor();
    }

    @Override
    public int getSkyColor() {
        return this.builder.getSpecialEffects().getSkyColor();
    }

    @Override
    public GrassColorModifier getGrassColorModifier() {
        return this.builder.getSpecialEffects().getGrassColorModifier();
    }

    @Override
    public Optional<Integer> getFoliageColorOverride() {
        return this.builder.getSpecialEffects().getFoliageColorOverride();
    }

    @Override
    public Optional<Integer> getGrassColorOverride() {
        return this.builder.getSpecialEffects().getGrassColorOverride();
    }

    @Override
    public Optional<AmbientParticleSettings> getAmbientParticleSettings() {
        return this.builder.getSpecialEffects().getAmbientParticle();
    }

    @Override
    public Optional<Holder<SoundEvent>> getAmbientLoopSound() {
        return this.builder.getSpecialEffects().getAmbientLoopSound();
    }

    @Override
    public Optional<AmbientMoodSettings> getAmbientMoodSettings() {
        return this.builder.getSpecialEffects().getAmbientMoodSound();
    }

    @Override
    public Optional<AmbientAdditionsSettings> getAmbientAdditions() {
        return this.builder.getSpecialEffects().getAmbientAdditionsSound();
    }

    @Override
    public Optional<Music> getBackgroundMusic() {
        return this.builder.getSpecialEffects().getBackgroundMusic();
    }

    @Override
    public void setFogColor(final int color) {
        this.builder.getSpecialEffects().fogColor(color);
        this.clientFeaturesModified();
    }

    @Override
    public void setWaterColor(final int color) {
        this.builder.getSpecialEffects().waterColor(color);
        this.clientFeaturesModified();
    }

    @Override
    public void setWaterFogColor(final int color) {
        this.builder.getSpecialEffects().waterFogColor(color);
        this.clientFeaturesModified();
    }

    @Override
    public void setSkyColor(final int color) {
        this.builder.getSpecialEffects().skyColor(color);
        this.clientFeaturesModified();
    }

    @Override
    public void setGrassColorModifier(final GrassColorModifier modifier) {
        this.builder.getSpecialEffects().grassColorModifier(modifier);
        this.clientFeaturesModified();
    }

    @Override
    public void setFoliageColorOverride(final @Nullable Integer override) {
        ((BiomeSpecialEffectsBuilderAccessor) this.builder.getSpecialEffects())
            .setFoliageColorOverride(Optional.ofNullable(override));
        this.clientFeaturesModified();
    }

    @Override
    public void setGrassColorOverride(final @Nullable Integer override) {
        ((BiomeSpecialEffectsBuilderAccessor) this.builder.getSpecialEffects())
            .setGrassColorOverride(Optional.ofNullable(override));
        this.clientFeaturesModified();
    }

    @Override
    public void setAmbientParticleSettings(final @Nullable AmbientParticleSettings settings) {
        ((BiomeSpecialEffectsBuilderAccessor) this.builder.getSpecialEffects())
            .setAmbientParticle(Optional.ofNullable(settings));
        this.clientFeaturesModified();
    }

    @Override
    public void setAmbientLoopSound(final @Nullable Holder<SoundEvent> event) {
        ((BiomeSpecialEffectsBuilderAccessor) this.builder.getSpecialEffects())
            .setAmbientLoopSoundEvent(Optional.ofNullable(event));
        this.clientFeaturesModified();
    }

    @Override
    public void setAmbientMoodSound(final @Nullable AmbientMoodSettings settings) {
        ((BiomeSpecialEffectsBuilderAccessor) this.builder.getSpecialEffects())
            .setAmbientMoodSettings(Optional.ofNullable(settings));
        this.clientFeaturesModified();
    }

    @Override
    public void setAmbientAdditionsSound(final @Nullable AmbientAdditionsSettings settings) {
        ((BiomeSpecialEffectsBuilderAccessor) this.builder.getSpecialEffects())
            .setAmbientAdditionsSettings(Optional.ofNullable(settings));
        this.clientFeaturesModified();
    }

    @Override
    public void setBackgroundMusic(final @Nullable Music music) {
        ((BiomeSpecialEffectsBuilderAccessor) this.builder.getSpecialEffects())
            .setBackgroundMusic(Optional.ofNullable(music));
        this.clientFeaturesModified();
    }

    @Override
    public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers(final Carving step) {
        return this.builder.getGenerationSettings().getCarvers(step);
    }

    @Override
    public List<Holder<PlacedFeature>> getFeatures(final Decoration step) {
        return this.builder.getGenerationSettings().getFeatures(step);
    }

    @Override
    public boolean removeCarver(final Carving step, final ResourceKey<ConfiguredWorldCarver<?>> key) {
        return this.builder.getGenerationSettings().getCarvers(step)
            .removeIf(holder -> key.equals(keyOf(this.carvers, holder)));
    }

    @Override
    public boolean removeCarver(final Carving step, final Predicate<Holder<ConfiguredWorldCarver<?>>> predicate) {
        return this.builder.getGenerationSettings().getCarvers(step).removeIf(predicate);
    }

    @Override
    public boolean removeFeature(final Decoration step, final ResourceKey<PlacedFeature> key) {
        return this.builder.getGenerationSettings().getFeatures(step)
            .removeIf(holder -> key.equals(keyOf(this.features, holder)));
    }

    @Override
    public boolean removeFeature(final Decoration step, final Predicate<Holder<PlacedFeature>> predicate) {
        return this.builder.getGenerationSettings().getFeatures(step).removeIf(predicate);
    }

    @Override
    public void addCarver(final Carving step, final Holder<ConfiguredWorldCarver<?>> carver) {
        this.builder.getGenerationSettings().addCarver(step, carver);
    }

    @Override
    public void addFeature(final Decoration step, final Holder<PlacedFeature> feature) {
        this.builder.getGenerationSettings().addFeature(step, feature);
    }

    @Override
    public Collection<MobCategory> getSpawnCategories() {
        return this.builder.getMobSpawnSettings().getSpawnerTypes();
    }

    @Override
    public Collection<EntityType<?>> getEntityTypes() {
        return this.builder.getMobSpawnSettings().getEntityTypes();
    }

    @Override
    public float getCreatureGenerationProbability() {
        return this.builder.getMobSpawnSettings().getProbability();
    }

    @Override
    public void setCreatureSpawnProbability(final float probability) {
        this.builder.getMobSpawnSettings().creatureGenerationProbability(probability);
    }

    @Override
    public void addSpawn(final MobCategory category, final SpawnerData data) {
        this.builder.getMobSpawnSettings().addSpawn(category, data);
    }

    @Override
    public void setSpawnCost(final EntityType<?> type, final double mass, final double gravityLimit) {
        this.builder.getMobSpawnSettings().addMobCharge(type, mass, gravityLimit);
    }

    @Override
    public void removeSpawns(final MobCategory category) {
        this.builder.getMobSpawnSettings().getSpawner(category).clear();
    }

    @Override
    public void removeSpawn(final EntityType<?> type) {
        for (final var category : MobCategory.values()) {
            this.builder.getMobSpawnSettings().getSpawner(category).removeIf(data -> data.type == type);
        }
    }

    @Override
    public void removeSpawnCost(final EntityType<?> type) {
        this.builder.getMobSpawnSettings().removeSpawnCost(type);
    }

    @Nullable
    private static <T> ResourceKey<T> keyOf(final Registry<T> registry, final Holder<T> holder) {
        return holder.unwrapKey().orElseGet(() -> registry.getResourceKey(holder.value()).orElse(null));
    }
}
