package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.*;
import personthecat.catlib.exception.BiomeNotFoundException;
import personthecat.catlib.exception.BiomeTypeNotFoundException;
import personthecat.catlib.exception.BlockNotFoundException;
import personthecat.catlib.exception.ItemNotFoundException;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustInherit;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static personthecat.catlib.exception.Exceptions.noBiomeNamed;
import static personthecat.catlib.exception.Exceptions.noBiomeTypeNamed;
import static personthecat.catlib.exception.Exceptions.noBlockNamed;
import static personthecat.catlib.exception.Exceptions.noItemNamed;

@UtilityClass
@OverwriteTarget
@SuppressWarnings("unused")
public class McTools {

    /**
     * @return A {@link File} pointing to the game's config directory.
     */
    @PlatformMustOverwrite
    public static File getConfigDir() {
        throw new MissingOverrideException();
    }

    /**
     * @param id The id of the mod being researched.
     * @return Whether the given mod is currently installed.
     */
    @PlatformMustOverwrite
    public static boolean isModLoaded(final String id) {
       throw new MissingOverrideException();
    }

    /**
     * @throws BlockNotFoundException If the block does not exist.
     * @param id The name of the block being researched.
     * @return The given block state, or else throws.
     */
    @NotNull
    @PlatformMustInherit
    public static BlockState assertBlockState(final String id) {
        return getBlockState(id).orElseThrow(() -> noBlockNamed(id));
    }

    /**
     * @param id The name of the block being researched.
     * @return The given block state, or else {@link Optional#empty}.
     */
    @PlatformMustOverwrite
    public static Optional<BlockState> getBlockState(final String id) {
        throw new MissingOverrideException();
    }

    /**
     * @throws ItemNotFoundException If the item does not exist.
     * @param id The name of the item being researched.
     * @return The given item, or else throws.
     */
    @NotNull
    @PlatformMustInherit
    public static Item assertItem(final String id) {
        return getItem(id).orElseThrow(() -> noItemNamed(id));
    }

    /**
     * @param id The name of the item being researched.
     * @return The given item, or else {@link Optional#empty}.
     */
    @PlatformMustOverwrite
    public static Optional<Item> getItem(final String id) {
        throw new MissingOverrideException();
    }

    /**
     * @throws BiomeNotFoundException If the biome does not exist.
     * @param id The name of the biome being researched.
     * @return The given biome, or else throws.
     */
    @NotNull
    @PlatformMustInherit
    public static Biome assertBiome(final String id) {
        return getBiome(id).orElseThrow(() -> noBiomeNamed(id));
    }

    /**
     * @param id The name of the biome being researched.
     * @return The given biome, or else {@link Optional#empty}.
     */
    @PlatformMustOverwrite
    public static Optional<Biome> getBiome(final String id) {
        throw new MissingOverrideException();
    }

    /**
     * @throws BiomeTypeNotFoundException If the biome type does not exist.
     * @param id The name of the biome type being researched.
     * @return The given biome type, or else throws.
     */
    @NotNull
    @PlatformMustInherit
    public static Biome.BiomeCategory assertBiomeType(final String id) {
        return getBiomeType(id).orElseThrow(() -> noBiomeTypeNamed(id));
    }

    /**
     * @param id The name of the biome type being researched.
     * @return The given biome type, or else {@link Optional#empty}.
     */
    @PlatformMustOverwrite
    public static Optional<Biome.BiomeCategory> getBiomeType(final String id) {
        throw new MissingOverrideException();
    }

    /**
     * @param type The type of biome being researched.
     * @return A list of biomes for the given category.
     */
    @PlatformMustOverwrite
    public static List<Biome> getBiomes(final Biome.BiomeCategory type) {
        throw new MissingOverrideException();
    }
}
