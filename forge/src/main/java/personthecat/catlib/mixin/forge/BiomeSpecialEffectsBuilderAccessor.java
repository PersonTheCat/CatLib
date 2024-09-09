package personthecat.catlib.mixin.forge;

import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(BiomeSpecialEffects.Builder.class)
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public interface BiomeSpecialEffectsBuilderAccessor {

    @Accessor
    void setFoliageColorOverride(final Optional<Integer> override);

    @Accessor
    void setGrassColorOverride(final Optional<Integer> override);

    @Accessor
    void setAmbientParticle(final Optional<AmbientParticleSettings> settings);

    @Accessor
    void setAmbientLoopSoundEvent(final Optional<Holder<SoundEvent>> event);

    @Accessor
    void setAmbientMoodSettings(final Optional<AmbientMoodSettings> settings);

    @Accessor
    void setAmbientAdditionsSettings(final Optional<AmbientAdditionsSettings> settings);

    @Accessor
    void setBackgroundMusic(final Optional<Music> music);

}
