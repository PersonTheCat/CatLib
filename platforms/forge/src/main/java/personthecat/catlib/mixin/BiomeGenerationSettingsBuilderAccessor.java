package personthecat.catlib.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(BiomeGenerationSettings.Builder.class)
public interface BiomeGenerationSettingsBuilderAccessor {
    @Accessor
    Map<GenerationStep.Carving, List<Holder<ConfiguredWorldCarver<?>>>> getCarvers();

    @Accessor
    List<List<Holder<PlacedFeature>>> getFeatures();
}
