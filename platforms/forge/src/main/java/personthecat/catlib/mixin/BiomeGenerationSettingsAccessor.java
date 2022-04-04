package personthecat.catlib.mixin;

import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(BiomeGenerationSettings.class)
public interface BiomeGenerationSettingsAccessor {

    @Accessor
    Map<GenerationStep.Carving, List<Supplier<ConfiguredWorldCarver<?>>>> getCarvers();

    @Accessor
    List<List<Supplier<ConfiguredFeature<?, ?>>>> getFeatures();

//    @Accessor
//    List<Supplier<ConfiguredStructureFeature<?, ?>>> getStructureStarts();

    @Mutable
    @Accessor
    void setCarvers(Map<GenerationStep.Carving, List<Supplier<ConfiguredWorldCarver<?>>>> carvers);

    @Mutable
    @Accessor
    void setFeatures(List<List<Supplier<ConfiguredFeature<?, ?>>>> features);

//    @Mutable
//    @Accessor
//    void setStructureStarts(List<Supplier<ConfiguredStructureFeature<?, ?>>> structures);
}
