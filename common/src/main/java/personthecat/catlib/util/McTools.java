package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.buildtools.OverwriteTarget;

import java.util.List;
import java.util.Optional;

@UtilityClass
@OverwriteTarget
@SuppressWarnings("unused")
public class McTools {

    public static Optional<BlockState> getBlockState(final String id) {
        throw new MissingOverrideException();
    }

    public static Optional<Biome> getBiome(final String id) {
        throw new MissingOverrideException();
    }

    public static Optional<Biome.BiomeCategory> getBiomeType(final String id) {
        throw new MissingOverrideException();
    }

    public static List<Biome> getBiomes(final Biome.BiomeCategory type) {
        throw new MissingOverrideException();
    }

}
