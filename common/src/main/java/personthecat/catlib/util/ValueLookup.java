package personthecat.catlib.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import java.util.Optional;
import java.util.Set;

/**
 * Used for serialization of non-enum static types.
 * <p>
 *   Note that MCP mappings are supported on the Forge platform
 *   for any type that differs between the two platforms.
 * </p>
 */
@SuppressWarnings("unused")
public final class ValueLookup {

    /** A map of every vanilla sound type to its name */
    private static final BiMap<String, SoundType> SOUND_MAP =
        ImmutableBiMap.<String, SoundType>builder()
            .put("EMPTY", SoundType.EMPTY)
            .put("WOOD", SoundType.WOOD)
            .put("GRAVEL", SoundType.GRAVEL)
            .put("GRASS", SoundType.GRASS)
            .put("LILY_PAD", SoundType.LILY_PAD)
            .put("STONE", SoundType.STONE)
            .put("METAL", SoundType.METAL)
            .put("GLASS", SoundType.GLASS)
            .put("WOOL", SoundType.WOOL)
            .put("SAND", SoundType.SAND)
            .put("SNOW", SoundType.SNOW)
            .put("POWDER_SNOW", SoundType.POWDER_SNOW)
            .put("LADDER", SoundType.LADDER)
            .put("ANVIL", SoundType.ANVIL)
            .put("SLIME_BLOCK", SoundType.SLIME_BLOCK)
            .put("HONEY_BLOCK", SoundType.HONEY_BLOCK)
            .put("WET_GRASS", SoundType.WET_GRASS)
            .put("CORAL_BLOCK", SoundType.CORAL_BLOCK)
            .put("BAMBOO", SoundType.BAMBOO)
            .put("BAMBOO_SAPLING", SoundType.BAMBOO_SAPLING)
            .put("SCAFFOLDING", SoundType.SCAFFOLDING)
            .put("SWEET_BERRY_BUSH", SoundType.SWEET_BERRY_BUSH)
            .put("CROP", SoundType.CROP)
            .put("HARD_CROP", SoundType.HARD_CROP)
            .put("VINE", SoundType.VINE)
            .put("NETHER_WART", SoundType.NETHER_WART)
            .put("LANTERN", SoundType.LANTERN)
            .put("STEM", SoundType.STEM)
            .put("NYLIUM", SoundType.NYLIUM)
            .put("FUNGUS", SoundType.FUNGUS)
            .put("ROOTS", SoundType.ROOTS)
            .put("SHROOMLIGHT", SoundType.SHROOMLIGHT)
            .put("WEEPING_VINES", SoundType.WEEPING_VINES)
            .put("TWISTING_VINES", SoundType.TWISTING_VINES)
            .put("SOUL_SAND", SoundType.SOUL_SAND)
            .put("SOUL_SOIL", SoundType.SOUL_SOIL)
            .put("BASALT", SoundType.BASALT)
            .put("WART_BLOCK", SoundType.WART_BLOCK)
            .put("NETHERRACK", SoundType.NETHERRACK)
            .put("NETHER_BRICK", SoundType.NETHER_BRICKS)
            .put("NETHER_SPROUT", SoundType.NETHER_SPROUTS)
            .put("NETHER_ORE", SoundType.NETHER_ORE)
            .put("BONE_BLOCK", SoundType.BONE_BLOCK)
            .put("NETHERITE_BLOCK", SoundType.NETHERITE_BLOCK)
            .put("ANCIENT_DEBRIS", SoundType.ANCIENT_DEBRIS)
            .put("LODESTONE", SoundType.LODESTONE)
            .put("CHAIN", SoundType.CHAIN)
            .put("NETHER_GOLD_ORE", SoundType.NETHER_GOLD_ORE)
            .put("GILDED_BLACKSTONE", SoundType.GILDED_BLACKSTONE)
            .put("CANDLE", SoundType.CANDLE)
            .put("AMETHYST", SoundType.AMETHYST)
            .put("AMETHYST_CLUSTER", SoundType.AMETHYST_CLUSTER)
            .put("SMALL_AMETHYST_BUD", SoundType.SMALL_AMETHYST_BUD)
            .put("MEDIUM_AMETHYST_BUD", SoundType.MEDIUM_AMETHYST_BUD)
            .put("LARGE_AMETHYST_BUD", SoundType.LARGE_AMETHYST_BUD)
            .put("TUFF", SoundType.TUFF)
            .put("TUFF_BRICKS", SoundType.TUFF_BRICKS)
            .put("POLISHED_TUFF", SoundType.POLISHED_TUFF)
            .put("CALCITE", SoundType.CALCITE)
            .put("DRIPSTONE_BLOCK", SoundType.DRIPSTONE_BLOCK)
            .put("COPPER", SoundType.COPPER)
            .put("COPPER_BULB", SoundType.COPPER_BULB)
            .put("COPPER_GRATE", SoundType.COPPER_GRATE)
            .put("CAVE_VINES", SoundType.CAVE_VINES)
            .put("SPORE_BLOSSOM", SoundType.SPORE_BLOSSOM)
            .put("AZALEA", SoundType.AZALEA)
            .put("FLOWERING_AZALEA", SoundType.FLOWERING_AZALEA)
            .put("MOSS_CARPET", SoundType.MOSS_CARPET)
            .put("PINK_PETALS", SoundType.PINK_PETALS)
            .put("MOSS", SoundType.MOSS)
            .put("BIG_DRIPLEAF", SoundType.BIG_DRIPLEAF)
            .put("ROOTED_DIRT", SoundType.ROOTED_DIRT)
            .put("HANGING_ROOTS", SoundType.HANGING_ROOTS)
            .put("AZALEA_LEAVES", SoundType.AZALEA_LEAVES)
            .put("SCULK_SENSOR", SoundType.SCULK_SENSOR)
            .put("SCULK_CATALYST", SoundType.SCULK_CATALYST)
            .put("SCULK", SoundType.SCULK)
            .put("SCULK_SHRIEKER", SoundType.SCULK_SHRIEKER)
            .put("GLOW_LICHEN", SoundType.GLOW_LICHEN)
            .put("DEEPSLATE", SoundType.DEEPSLATE)
            .put("DEEPSLATE_BRICKS", SoundType.DEEPSLATE_BRICKS)
            .put("DEEPSLATE_TILES", SoundType.DEEPSLATE_TILES)
            .put("POLISHED_DEEPSLATE", SoundType.POLISHED_DEEPSLATE)
            .put("FROGLIGHT", SoundType.FROGLIGHT)
            .put("FROGSPAWN", SoundType.FROGSPAWN)
            .put("MANGROVE_ROOTS", SoundType.MANGROVE_ROOTS)
            .put("MUDDY_MANGROVE_ROOTS", SoundType.MUDDY_MANGROVE_ROOTS)
            .put("MUD", SoundType.MUD)
            .put("MUD_BRICKS", SoundType.MUD_BRICKS)
            .put("PACKED_MUD", SoundType.PACKED_MUD)
            .put("HANGING_SIGN", SoundType.HANGING_SIGN)
            .put("NETHER_WOOD_HANGING_SIGN", SoundType.NETHER_WOOD_HANGING_SIGN)
            .put("BAMBOO_WOOD_HANGING_SIGN", SoundType.BAMBOO_WOOD_HANGING_SIGN)
            .put("BAMBOO_WOOD", SoundType.BAMBOO_WOOD)
            .put("NETHER_WOOD", SoundType.NETHER_WOOD)
            .put("CHERRY_WOOD", SoundType.CHERRY_WOOD)
            .put("CHERRY_SAPLING", SoundType.CHERRY_SAPLING)
            .put("CHERRY_LEAVES", SoundType.CHERRY_LEAVES)
            .put("CHERRY_WOOD_HANGING_SIGN", SoundType.CHERRY_WOOD_HANGING_SIGN)
            .put("CHISELED_BOOKSHELF", SoundType.CHISELED_BOOKSHELF)
            .put("SUSPICIOUS_SAND", SoundType.SUSPICIOUS_SAND)
            .put("SUSPICIOUS_GRAVEL", SoundType.SUSPICIOUS_GRAVEL)
            .put("DECORATED_POT", SoundType.DECORATED_POT)
            .put("DECORATED_POT_CRACKED", SoundType.DECORATED_POT_CRACKED)
            .put("TRIAL_SPAWNER", SoundType.TRIAL_SPAWNER)
            .put("SPONGE", SoundType.SPONGE)
            .put("WET_SPONGE", SoundType.WET_SPONGE)
            .build();

    /** A map of every vanilla material color to its name. */
    private static final BiMap<String, MapColor> COLOR_MAP =
        ImmutableBiMap.<String, MapColor>builder()
            .put("NONE", MapColor.NONE)
            .put("GRASS", MapColor.GRASS)
            .put("SAND", MapColor.SAND)
            .put("WOOL", MapColor.WOOL)
            .put("FIRE", MapColor.FIRE)
            .put("ICE", MapColor.ICE)
            .put("METAL", MapColor.METAL)
            .put("PLANT", MapColor.PLANT)
            .put("SNOW", MapColor.SNOW)
            .put("CLAY", MapColor.CLAY)
            .put("DIRT", MapColor.DIRT)
            .put("STONE", MapColor.STONE)
            .put("WATER", MapColor.WATER)
            .put("WOOD", MapColor.WOOD)
            .put("QUARTZ", MapColor.QUARTZ)
            .put("COLOR_ORANGE", MapColor.COLOR_ORANGE)
            .put("COLOR_MAGENTA", MapColor.COLOR_MAGENTA)
            .put("COLOR_LIGHT_BLUE", MapColor.COLOR_LIGHT_BLUE)
            .put("COLOR_YELLOW", MapColor.COLOR_YELLOW)
            .put("COLOR_LIGHT_GREEN", MapColor.COLOR_LIGHT_GREEN)
            .put("COLOR_PINK", MapColor.COLOR_PINK)
            .put("COLOR_GRAY", MapColor.COLOR_GRAY)
            .put("COLOR_LIGHT_GRAY", MapColor.COLOR_LIGHT_GRAY)
            .put("COLOR_CYAN", MapColor.COLOR_CYAN)
            .put("COLOR_PURPLE", MapColor.COLOR_PURPLE)
            .put("COLOR_BLUE", MapColor.COLOR_BLUE)
            .put("COLOR_BROWN", MapColor.COLOR_BROWN)
            .put("COLOR_GREEN", MapColor.COLOR_GREEN)
            .put("COLOR_RED", MapColor.COLOR_RED)
            .put("COLOR_BLACK", MapColor.COLOR_BLACK)
            .put("GOLD", MapColor.GOLD)
            .put("DIAMOND", MapColor.DIAMOND)
            .put("LAPIS", MapColor.LAPIS)
            .put("EMERALD", MapColor.EMERALD)
            .put("PODZOL", MapColor.PODZOL)
            .put("NETHER", MapColor.NETHER)
            .put("TERRACOTTA_WHITE", MapColor.TERRACOTTA_WHITE)
            .put("TERRACOTTA_ORANGE", MapColor.TERRACOTTA_ORANGE)
            .put("TERRACOTTA_MAGENTA", MapColor.TERRACOTTA_MAGENTA)
            .put("TERRACOTTA_LIGHT_BLUE", MapColor.TERRACOTTA_LIGHT_BLUE)
            .put("TERRACOTTA_YELLOW", MapColor.TERRACOTTA_YELLOW)
            .put("TERRACOTTA_LIGHT_GREEN", MapColor.TERRACOTTA_LIGHT_GREEN)
            .put("TERRACOTTA_PINK", MapColor.TERRACOTTA_PINK)
            .put("TERRACOTTA_GRAY", MapColor.TERRACOTTA_GRAY)
            .put("TERRACOTTA_LIGHT_GRAY", MapColor.TERRACOTTA_LIGHT_GRAY)
            .put("TERRACOTTA_CYAN", MapColor.TERRACOTTA_CYAN)
            .put("TERRACOTTA_PURPLE", MapColor.TERRACOTTA_PURPLE)
            .put("TERRACOTTA_BLUE", MapColor.TERRACOTTA_BLUE)
            .put("TERRACOTTA_BROWN", MapColor.TERRACOTTA_BROWN)
            .put("TERRACOTTA_GREEN", MapColor.TERRACOTTA_GREEN)
            .put("TERRACOTTA_RED", MapColor.TERRACOTTA_RED)
            .put("TERRACOTTA_BLACK", MapColor.TERRACOTTA_BLACK)
            .put("CRIMSON_NYLIUM", MapColor.CRIMSON_NYLIUM)
            .put("CRIMSON_STEM", MapColor.CRIMSON_STEM)
            .put("CRIMSON_HYPHAE", MapColor.CRIMSON_HYPHAE)
            .put("WARPED_NYLIUM", MapColor.WARPED_NYLIUM)
            .put("WARPED_STEM", MapColor.WARPED_STEM)
            .put("WARPED_HYPHAE", MapColor.WARPED_HYPHAE)
            .put("WARPED_WART_BLOCK", MapColor.WARPED_WART_BLOCK)
            .put("DEEPSLATE", MapColor.DEEPSLATE)
            .put("RAW_IRON", MapColor.RAW_IRON)
            .put("GLOW_LICHEN", MapColor.GLOW_LICHEN)
            .build();

    /** A regular codec used for serializing material colors in config files. */
    public static Codec<MapColor> COLOR_CODEC = Codec.STRING.flatXmap(
        key -> getColor(key).map(DataResult::success).orElse(DataResult.error(() -> "No such color: " + key)),
        color -> serialize(color).map(DataResult::success).orElse(DataResult.error(() -> "Unknown color: " + color))
    );

    /** A regular codec used for serializing sound types in config files. */
    public static Codec<SoundType> SOUND_CODEC = Codec.STRING.flatXmap(
        key -> getSoundType(key).map(DataResult::success).orElse(DataResult.error(() -> "No such sound type: " + key)),
        sound -> serialize(sound).map(DataResult::success).orElse(DataResult.error(() -> "Unknown sound type: " + sound))
    );

    private ValueLookup() {}

    /**
     * Attempts to retrieve a sound type from the map.
     *
     * @param key The name of the sound type being queried.
     * @return The corresponding {@link SoundType}, or else {@link Optional#empty}.
     */
    public static Optional<SoundType> getSoundType(final String key) {
        return Optional.ofNullable(SOUND_MAP.get(key.toUpperCase()));
    }

    /**
     * Returns every sound name in the registry.
     *
     * @return All sound keys.
     */
    public static Set<String> getSoundNames() {
        return SOUND_MAP.keySet();
    }

    /**
     * Returns every sound object in the registry.
     *
     * @return All known sound types.
     */
    public static Set<SoundType> getSoundValues() {
        return SOUND_MAP.values();
    }

    /**
     * Converts the input sound type to a string.
     *
     * @param value The actual {@link SoundType} object which may have a name.
     * @return The name of the sound, or else {@link Optional#empty}.
     */
    public static Optional<String> serialize(final SoundType value) {
        return Optional.ofNullable(SOUND_MAP.inverse().get(value));
    }

    /**
     * Attempts to retrieve a material color from the map.
     *
     * @param key The name of the color being queried.
     * @return The actual color object, or else {@link Optional#empty}.
     */
    public static Optional<MapColor> getColor(final String key) {
        return Optional.ofNullable(COLOR_MAP.get(key.toUpperCase()));
    }

    /**
     * Returns every color name in the registry.
     *
     * @return All known color keys.
     */
    public static Set<String> getColorNames() {
        return COLOR_MAP.keySet();
    }

    /**
     * Returns every color object in the registry.
     *
     * @return All known color values.
     */
    public static Set<MapColor> getColorValues() {
        return COLOR_MAP.values();
    }

    /**
     * Converts the input color type to a string.
     *
     * @param value The actual {@link MapColor} which may have a name.
     * @return The name of the color, or else {@link Optional#empty}.
     */
    public static Optional<String> serialize(final MapColor value) {
        return Optional.ofNullable(COLOR_MAP.inverse().get(value));
    }
}