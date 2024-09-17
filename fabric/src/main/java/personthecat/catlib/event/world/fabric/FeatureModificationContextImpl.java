package personthecat.catlib.event.world.fabric;

import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.ClimateSettings;
import net.minecraft.world.level.biome.Biome.TemperatureModifier;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.world.FeatureModificationContext;
import personthecat.catlib.mixin.fabric.BiomeModificationContextAccessor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class FeatureModificationContextImpl extends FeatureModificationContext {

    private final BiomeSelectionContext biome;
    private final BiomeModificationContext modifications;
    private final Registry<ConfiguredWorldCarver<?>> carvers;
    private final Registry<PlacedFeature> features;
    private final RegistryAccess registries;

    public FeatureModificationContextImpl(final BiomeSelectionContext biome, final BiomeModificationContext ctx) {
        final RegistryAccess registries = ((BiomeModificationContextAccessor) ctx).getRegistries();
        this.biome = biome;
        this.modifications = ctx;
        this.carvers = registries.registryOrThrow(Registries.CONFIGURED_CARVER);
        this.features = registries.registryOrThrow(Registries.PLACED_FEATURE);
        this.registries = registries;
    }

    @Override
    public Holder<Biome> getBiome() {
        return this.biome.getBiomeRegistryEntry();
    }

    @Override
    public ResourceLocation getName() {
        return this.biome.getBiomeKey().location();
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

    // Fabric overwrites these values each time they're updated, so we have to re-access
    private ClimateSettings getClimate() {
        return this.getBiome().value().climateSettings;
    }

    private BiomeGenerationSettings getGeneration() {
        return this.getBiome().value().getGenerationSettings();
    }

    private MobSpawnSettings getMobs() {
        return this.getBiome().value().getMobSettings();
    }

    private BiomeSpecialEffects getEffects() {
        return this.getBiome().value().getSpecialEffects();
    }

    @Override
    public boolean hasPrecipitation() {
        return this.getClimate().hasPrecipitation();
    }

    @Override
    public float getTemperature() {
        return this.getClimate().temperature();
    }

    @Override
    public TemperatureModifier getTemperatureModifier() {
        return this.getClimate().temperatureModifier();
    }

    @Override
    public float getDownfall() {
        return this.getClimate().downfall();
    }

    @Override
    public void setHasPrecipitation(final boolean hasPrecipitation) {
        this.modifications.getWeather().setPrecipitation(hasPrecipitation);
    }

    @Override
    public void setTemperature(final float temperature) {
        this.modifications.getWeather().setTemperature(temperature);
    }

    @Override
    public void setTemperatureModifier(final TemperatureModifier modifier) {
        this.modifications.getWeather().setTemperatureModifier(modifier);
    }

    @Override
    public void setDownfall(final float downfall) {
        this.modifications.getWeather().setDownfall(downfall);
    }

    @Override
    public int getFogColor() {
        return this.getEffects().getFogColor();
    }

    @Override
    public int getWaterColor() {
        return this.getEffects().getWaterColor();
    }

    @Override
    public int getWaterFogColor() {
        return this.getEffects().getWaterFogColor();
    }

    @Override
    public int getSkyColor() {
        return this.getEffects().getSkyColor();
    }

    @Override
    public GrassColorModifier getGrassColorModifier() {
        return this.getEffects().getGrassColorModifier();
    }

    @Override
    public Optional<Integer> getFoliageColorOverride() {
        return this.getEffects().getFoliageColorOverride();
    }

    @Override
    public Optional<Integer> getGrassColorOverride() {
        return this.getEffects().getGrassColorOverride();
    }

    @Override
    public Optional<AmbientParticleSettings> getAmbientParticleSettings() {
        return this.getEffects().getAmbientParticleSettings();
    }

    @Override
    public Optional<Holder<SoundEvent>> getAmbientLoopSound() {
        return this.getEffects().getAmbientLoopSoundEvent();
    }

    @Override
    public Optional<AmbientMoodSettings> getAmbientMoodSettings() {
        return this.getEffects().getAmbientMoodSettings();
    }

    @Override
    public Optional<AmbientAdditionsSettings> getAmbientAdditions() {
        return this.getEffects().getAmbientAdditionsSettings();
    }

    @Override
    public Optional<Music> getBackgroundMusic() {
        return this.getEffects().getBackgroundMusic();
    }

    @Override
    public void setFogColor(final int color) {
        this.modifications.getEffects().setFogColor(color);
    }

    @Override
    public void setWaterColor(final int color) {
        this.modifications.getEffects().setWaterColor(color);
    }

    @Override
    public void setWaterFogColor(final int color) {
        this.modifications.getEffects().setWaterFogColor(color);
    }

    @Override
    public void setSkyColor(final int color) {
        this.modifications.getEffects().setSkyColor(color);
    }

    @Override
    public void setGrassColorModifier(final GrassColorModifier modifier) {
        this.modifications.getEffects().setGrassColorModifier(modifier);
    }

    @Override
    public void setFoliageColorOverride(final @Nullable Integer override) {
        this.modifications.getEffects().setFoliageColor(Optional.ofNullable(override));
    }

    @Override
    public void setGrassColorOverride(final @Nullable Integer override) {
        this.modifications.getEffects().setGrassColor(Optional.ofNullable(override));
    }

    @Override
    public void setAmbientParticleSettings(final @Nullable AmbientParticleSettings settings) {
        this.modifications.getEffects().setParticleConfig(Optional.ofNullable(settings));
    }

    @Override
    public void setAmbientLoopSound(final @Nullable Holder<SoundEvent> event) {
        this.modifications.getEffects().setAmbientSound(Optional.ofNullable(event));
    }

    @Override
    public void setAmbientMoodSound(final @Nullable AmbientMoodSettings settings) {
        this.modifications.getEffects().setMoodSound(Optional.ofNullable(settings));
    }

    @Override
    public void setAmbientAdditionsSound(final @Nullable AmbientAdditionsSettings settings) {
        this.modifications.getEffects().setAdditionsSound(Optional.ofNullable(settings));
    }

    @Override
    public void setBackgroundMusic(final @Nullable Music music) {
        this.modifications.getEffects().setMusic(Optional.ofNullable(music));
    }

    @Override
    public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers(final Carving step) {
        return this.getGeneration().getCarvers(step);
    }

    @Override
    public HolderSet<PlacedFeature> getFeatures(final Decoration step) {
        final List<HolderSet<PlacedFeature>> features = this.getGeneration().features();
        while (features.size() <= step.ordinal()) {
            features.add(HolderSet.direct(Collections.emptyList()));
        }
        return this.getGeneration().features().get(step.ordinal());
    }

    @Override
    public boolean removeCarver(final Carving step, final ResourceLocation id) {
        return this.modifications.getGenerationSettings().removeCarver(step, ResourceKey.create(Registries.CONFIGURED_CARVER, id));
    }

    @Override
    public boolean removeCarver(final Carving step, final Predicate<ConfiguredWorldCarver<?>> predicate) {
        boolean anyRemoved = false;
        for (final Map.Entry<ResourceKey<ConfiguredWorldCarver<?>>, ConfiguredWorldCarver<?>> entry : this.carvers.entrySet()) {
            if (predicate.test(entry.getValue())) {
                anyRemoved |= this.modifications.getGenerationSettings().removeCarver(step, entry.getKey());
            }
        }
        return anyRemoved;
    }

    @Override
    public boolean removeFeature(final Decoration step, final ResourceLocation id) {
        return this.modifications.getGenerationSettings().removeFeature(step, ResourceKey.create(Registries.PLACED_FEATURE, id));
    }

    @Override
    public boolean removeFeature(final Decoration step, final Predicate<PlacedFeature> predicate) {
        boolean anyRemoved = false;
        for (final Map.Entry<ResourceKey<PlacedFeature>, PlacedFeature> entry : this.features.entrySet()) {
             if (predicate.test(entry.getValue())) {
                 anyRemoved |= this.modifications.getGenerationSettings().removeFeature(step, entry.getKey());
             }
         }
         return anyRemoved;
    }

    @Override
    public void addCarver(final Carving step, final ConfiguredWorldCarver<?> carver) {
        final ResourceKey<ConfiguredWorldCarver<?>> key = this.carvers.getResourceKey(carver)
            .orElseThrow(() -> new NullPointerException("Carvers must be registered prior to feature modification"));
        this.modifications.getGenerationSettings().addCarver(step, key);
    }

    @Override
    public void addFeature(final Decoration step, final PlacedFeature feature) {
        final ResourceKey<PlacedFeature> key = this.features.getResourceKey(feature)
            .orElseThrow(() -> new NullPointerException("Features must be registered prior to feature modification"));
        this.modifications.getGenerationSettings().addFeature(step, key);
    }

    @Override
    public Collection<MobCategory> getSpawnCategories() {
        return this.getMobs().spawners.keySet();
    }

    @Override
    public Collection<EntityType<?>> getEntityTypes() {
        return this.getMobs().mobSpawnCosts.keySet();
    }

    @Override
    public float getCreatureGenerationProbability() {
        return this.getMobs().creatureGenerationProbability;
    }

    @Override
    public void addSpawn(final MobCategory category, final SpawnerData data) {
        this.modifications.getSpawnSettings().addSpawn(category, data);
    }

    @Override
    public void setSpawnCost(final EntityType<?> type, final double mass, final double gravityLimit) {
        this.modifications.getSpawnSettings().setSpawnCost(type, mass, gravityLimit);
    }

    @Override
    public void removeSpawn(final MobCategory category) {
        this.modifications.getSpawnSettings().clearSpawns(category);
    }

    @Override
    public void removeSpawnCost(final EntityType<?> type) {
        this.modifications.getSpawnSettings().clearSpawnCost(type);
    }
}

