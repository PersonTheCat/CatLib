package personthecat.catlib.event.world.forge;

import com.google.common.base.Suppliers;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.WorldData;
import org.jetbrains.annotations.ApiStatus;
import personthecat.catlib.mixin.forge.BiomeSourceAccessor;

import java.util.stream.Collectors;

@ApiStatus.Internal
public class BiomeCacheInvalidator {

    // From Fabric: fixes feature order after modifications
    public static void invalidateBiomes(final WorldData data) {
        for (LevelStem dimension : data.worldGenSettings().dimensions()) {
            final BiomeSourceAccessor accessor = (BiomeSourceAccessor) dimension.generator().getBiomeSource();
            accessor.setFeaturesPerStep(Suppliers.memoize(() ->
                accessor.invokeBuildFeaturesPerStep(
                    accessor.getPossibleBiomes().stream().distinct().collect(Collectors.toList()), true)));
        }
    }
}
