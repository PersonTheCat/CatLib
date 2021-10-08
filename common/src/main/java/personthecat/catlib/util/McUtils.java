package personthecat.catlib.util;

import com.mojang.brigadier.StringReader;
import lombok.experimental.UtilityClass;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.event.registry.DynamicRegistries;
import personthecat.catlib.event.registry.RegistryHandle;
import personthecat.catlib.exception.BiomeNotFoundException;
import personthecat.catlib.exception.BiomeTypeNotFoundException;
import personthecat.catlib.exception.BlockNotFoundException;
import personthecat.catlib.exception.ItemNotFoundException;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.fresult.Result;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustInherit;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static personthecat.catlib.exception.Exceptions.noBiomeNamed;
import static personthecat.catlib.exception.Exceptions.noBiomeTypeNamed;
import static personthecat.catlib.exception.Exceptions.noBlockNamed;
import static personthecat.catlib.exception.Exceptions.noItemNamed;
import static personthecat.catlib.util.Shorthand.getEnumConstant;

@UtilityClass
@OverwriteTarget(required = true)
@SuppressWarnings("unused")
public class McUtils {

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
     * @return Whether the game is currently running on the client side.
     */
    @PlatformMustInherit
    public static boolean isClientSide() {
        return !isDedicatedServer();
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
     * @apiNote The implementation of this method may differ on each platform.
     * @return A set of all registered blocks which can be iterated though.
     */
    @NotNull
    public static Iterable<Block> getAllBlocks() {
        return Registry.BLOCK;
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
     * @throws BlockNotFoundException If the block does not exist or the format is invalid.
     * @param state The raw block state input.
     * @return The expected state.
     */
    @NotNull
    public static BlockState assertParseBlockState(final String state) {
        return parseBlockState(state).orElseThrow(() -> noBlockNamed(state));
    }

    /**
     * Parses a block state in the format expected by <code>/setblock</code>.
     *
     * <p>For example,</p>
     * <ul>
     *   <li>minecraft:stone</li>
     *   <li>oak_log[axis=x]</li>
     * </ul>
     *
     * @param state The raw block state text.
     * @return The expected state, or else {@link Optional#empty}.
     */
    public static Optional<BlockState> parseBlockState(final String state) {
        final BlockStateParser parser = new BlockStateParser(new StringReader(state), true);
        return Result.suppress(() -> parser.parse(false).getState()).get(Result::IGNORE);
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
     * @apiNote The implementation of this method may differ on each platform.
     * @return A set of all registered items which can be iterated though.
     */
    @NotNull
    public static Iterable<Item> getAllItems() {
        return Registry.ITEM;
    }

    /**
     * @throws ItemNotFoundException If the item does not exist or the format is invalid.
     * @param item The raw item input being parsed.
     * @return The expected item.
     */
    @NotNull
    public static Item assertParseItem(final String item) {
        return parseItem(item).orElseThrow(() -> noItemNamed(item));
    }

    /**
     * Parses an item in the format expected by <code>/give</code>.
     *
     * <p>For example,</p>
     * <ul>
     *   <li>minecraft:stone</li>
     *   <li>minecraft:chest{...}</li>
     * </ul>
     *
     * @param item The raw item input being parsed.
     * @return The expected item, or else {@link Optional#empty}.
     */
    public static Optional<Item> parseItem(final String item) {
        final ItemParser parser = new ItemParser(new StringReader(item), true);
        return Result.suppress(() -> parser.parse().getItem()).get(Result::IGNORE);
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
     * @param id The name of the biome being researched.
     * @return The given biome, or else {@link Optional#empty}.
     */
    public static Optional<Biome> getBiome(final ResourceLocation id) {
        return Optional.ofNullable(DynamicRegistries.BIOMES.lookup(id));
    }

    /**
     * @apiNote The implementation of this method may differ on each platform.
     * @return A set of all registered biomes which can be iterated though.
     */
    public static RegistryHandle<Biome> getAllBiomes() {
        return DynamicRegistries.BIOMES;
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
     * @param type The type of biome being researched.
     * @return A list of biomes for the given category.
     */
    public static List<Biome> getBiomes(final Biome.BiomeCategory type) {
        return StreamSupport.stream(DynamicRegistries.BIOMES.spliterator(), false)
            .filter(biome -> biome.getBiomeCategory().equals(type))
            .collect(Collectors.toList());
    }
}
