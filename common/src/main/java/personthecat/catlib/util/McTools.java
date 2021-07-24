package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
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
import java.util.stream.Collectors;

import static personthecat.catlib.exception.Exceptions.noBiomeNamed;
import static personthecat.catlib.exception.Exceptions.noBiomeTypeNamed;
import static personthecat.catlib.exception.Exceptions.noBlockNamed;
import static personthecat.catlib.exception.Exceptions.noItemNamed;
import static personthecat.catlib.util.Shorthand.getEnumConstant;

@UtilityClass
@OverwriteTarget(required = true)
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
     * @return Whether the game is currently running on a dedicated server.
     */
    @PlatformMustOverwrite
    public static boolean isDedicatedServer() {
        throw new MissingOverrideException();
    }

    /**
     * @throws BlockNotFoundException If the block does not exist.
     * @param id The name of the block being researched.
     * @return The given block, or else throws.
     */
    @NotNull
    @PlatformMustInherit
    public static Block assertBlock(final ResourceLocation id) {
        return getBlock(id).orElseThrow(() -> noBlockNamed(id.toString()));
    }

    /**
     * @param id The name of the block being researched.
     * @return The given block, or else {@link Optional#empty}.
     */
    public static Optional<Block> getBlock(final ResourceLocation id) {
        return Registry.BLOCK.getOptional(id);
    }

    /**
     * @throws BlockNotFoundException If the block does not exist.
     * @param id The name of the block being researched.
     * @return The given block's default state, or else throws.
     */
    @NotNull
    @PlatformMustInherit
    public static BlockState assertBlockState(final ResourceLocation id) {
        return assertBlock(id).defaultBlockState();
    }

    /**
     * @param id The name of the block being researched.
     * @return The given block's default state, or else {@link Optional#empty}.
     */
    @PlatformMustInherit
    public static Optional<BlockState> getBlockState(final ResourceLocation id) {
        return getBlock(id).map(Block::defaultBlockState);
    }

    /**
     * @throws ItemNotFoundException If the item does not exist.
     * @param id The name of the item being researched.
     * @return The given item, or else throws.
     */
    @NotNull
    @PlatformMustInherit
    public static Item assertItem(final ResourceLocation id) {
        return getItem(id).orElseThrow(() -> noItemNamed(id.toString()));
    }

    /**
     * @param id The name of the item being researched.
     * @return The given item, or else {@link Optional#empty}.
     */
    public static Optional<Item> getItem(final ResourceLocation id) {
        return Registry.ITEM.getOptional(id);
    }

    /**
     * @throws BiomeNotFoundException If the biome does not exist.
     * @param id The name of the biome being researched.
     * @return The given biome, or else throws.
     */
    @NotNull
    @PlatformMustInherit
    public static Biome assertBiome(final ResourceLocation id) {
        return getBiome(id).orElseThrow(() -> noBiomeNamed(id.toString()));
    }

    /**
     * Todo: consider data pack biomes.
     *
     * @param id The name of the biome being researched.
     * @return The given biome, or else {@link Optional#empty}.
     */
    public static Optional<Biome> getBiome(final ResourceLocation id) {
        return BuiltinRegistries.BIOME.getOptional(id);
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
    public static Optional<Biome.BiomeCategory> getBiomeType(final String id) {
        return getEnumConstant(id, Biome.BiomeCategory.class);
    }

    /**
     * Todo: consider data pack biomes.
     *
     * @param type The type of biome being researched.
     * @return A list of biomes for the given category.
     */
    public static List<Biome> getBiomes(final Biome.BiomeCategory type) {
        return BuiltinRegistries.BIOME.stream()
            .filter(b -> type.equals(b.getBiomeCategory()))
            .collect(Collectors.toList());
    }
}
