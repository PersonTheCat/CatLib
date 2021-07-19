package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.io.File;
import java.util.List;
import java.util.Optional;

@UtilityClass
@OverwriteClass
@SuppressWarnings("unused")
public class McTools {

    @Overwrite // Todo: Missing implementation
    public static File getConfigDir() {
        throw new MissingOverrideException();
    }

    @Overwrite // Todo: Missing implementation
    public static boolean isModLoaded(final String id) {
        throw new MissingOverrideException();
    }

    @NotNull
    @Inherit
    public static Block assertBlock(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @Inherit
    public static Optional<Block> getBlock(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @NotNull
    @Inherit
    public static BlockState assertBlockState(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @Inherit
    public static Optional<BlockState> getBlockState(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @NotNull
    @Inherit
    public static Item assertItem(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @Inherit
    public static Optional<Item> getItem(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @NotNull
    @Inherit
    public static Biome assertBiome(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @Inherit
    public static Optional<Biome> getBiome(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @NotNull
    @Inherit
    public static Biome.BiomeCategory assertBiomeType(final String id) {
        throw new MissingOverrideException();
    }

    @Inherit
    public static Optional<Biome.BiomeCategory> getBiomeType(final String id) {
        throw new MissingOverrideException();
    }

    @Inherit
    public static List<Biome> getBiomes(final Biome.BiomeCategory type) {
        throw new MissingOverrideException();
    }
}
