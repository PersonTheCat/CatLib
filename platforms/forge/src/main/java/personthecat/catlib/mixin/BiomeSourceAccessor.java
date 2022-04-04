package personthecat.catlib.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeSource.StepFeatureData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Mixin(BiomeSource.class)
public interface BiomeSourceAccessor {

    @Mutable
    @Accessor
    void setFeaturesPerStep(final Supplier<List<StepFeatureData>> features);

    @Accessor
    Set<Holder<Biome>> getPossibleBiomes();

    @Invoker
    List<StepFeatureData> invokeBuildFeaturesPerStep(final List<Holder<Biome>> biomes, final boolean flexible);
}
