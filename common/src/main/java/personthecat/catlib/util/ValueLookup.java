package personthecat.catlib.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.experimental.UtilityClass;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import personthecat.overwritevalidator.annotations.OverwriteTarget;

import java.util.Optional;
import java.util.Set;

import static personthecat.catlib.util.Shorthand.nullable;

/**
 * Used for serialization of non-enum static types.
 * <p>
 *   Note that MCP mappings are supported on the Forge platform
 *   for any type that differs between the two platforms.
 * </p>
 */
@UtilityClass
@OverwriteTarget
@SuppressWarnings("unused")
public class ValueLookup {

    /** A map of every vanilla material to its name. */
    private static final BiMap<String, Material> MATERIAL_MAP = ImmutableBiMap.<String, Material>builder()
        .put("AIR", Material.AIR)
        .put("STRUCTURAL_AIR", Material.STRUCTURAL_AIR)
        .put("PORTAL", Material.PORTAL)
        .put("CLOTH_DECORATION", Material.CLOTH_DECORATION)
        .put("PLANT", Material.PLANT)
        .put("WATER_PLANT", Material.WATER_PLANT)
        .put("REPLACEABLE_PLANT", Material.REPLACEABLE_PLANT)
        .put("REPLACEABLE_FIREPROOF_PLANT", Material.REPLACEABLE_FIREPROOF_PLANT)
        .put("REPLACEABLE_WATER_PLANT", Material.REPLACEABLE_WATER_PLANT)
        .put("WATER", Material.WATER)
        .put("BUBBLE_COLUMN", Material.BUBBLE_COLUMN)
        .put("LAVA", Material.LAVA)
        .put("TOP_SNOW", Material.TOP_SNOW)
        .put("FIRE", Material.FIRE)
        .put("DECORATION", Material.DECORATION)
        .put("WEB", Material.WEB)
        .put("BUILDABLE_GLASS", Material.BUILDABLE_GLASS)
        .put("CLAY", Material.CLAY)
        .put("DIRT", Material.DIRT)
        .put("GRASS", Material.GRASS)
        .put("ICE_SOLID", Material.ICE_SOLID)
        .put("SAND", Material.SAND)
        .put("SPONGE", Material.SPONGE)
        .put("SHULKER_SHELL", Material.SHULKER_SHELL)
        .put("WOOD", Material.WOOD)
        .put("NETHER_WOOD", Material.NETHER_WOOD)
        .put("BAMBOO_SAPLING", Material.BAMBOO_SAPLING)
        .put("BAMBOO", Material.BAMBOO)
        .put("WOOL", Material.WOOL)
        .put("EXPLOSIVE", Material.EXPLOSIVE)
        .put("LEAVES", Material.LEAVES)
        .put("GLASS", Material.GLASS)
        .put("ICE", Material.ICE)
        .put("CACTUS", Material.CACTUS)
        .put("STONE", Material.STONE)
        .put("METAL", Material.METAL)
        .put("SNOW", Material.SNOW)
        .put("HEAVY_METAL", Material.HEAVY_METAL)
        .put("BARRIER", Material.BARRIER)
        .put("PISTON", Material.PISTON)
        .put("CORAL", Material.CORAL)
        .put("VEGETABLE", Material.VEGETABLE)
        .put("EGG", Material.EGG)
        .put("CAKE", Material.CAKE)
        .build();

    /** A map of every vanilla sound type to its name */
    private static final BiMap<String, SoundType> SOUND_MAP = ImmutableBiMap.<String, SoundType>builder()
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
        .build();

    /** A map of every vanilla material color to its name. */
    private static final BiMap<String, MaterialColor> COLOR_MAP = ImmutableBiMap.<String, MaterialColor>builder()
        .put("NONE", MaterialColor.NONE)
        .put("GRASS", MaterialColor.GRASS)
        .put("SAND", MaterialColor.SAND)
        .put("WOOL", MaterialColor.WOOL)
        .put("FIRE", MaterialColor.FIRE)
        .put("ICE", MaterialColor.ICE)
        .put("METAL", MaterialColor.METAL)
        .put("PLANT", MaterialColor.PLANT)
        .put("SNOW", MaterialColor.SNOW)
        .put("CLAY", MaterialColor.CLAY)
        .put("DIRT", MaterialColor.DIRT)
        .put("STONE", MaterialColor.STONE)
        .put("WATER", MaterialColor.WATER)
        .put("WOOD", MaterialColor.WOOD)
        .put("QUARTZ", MaterialColor.QUARTZ)
        .put("COLOR_ORANGE", MaterialColor.COLOR_ORANGE)
        .put("COLOR_MAGENTA", MaterialColor.COLOR_MAGENTA)
        .put("COLOR_LIGHT_BLUE", MaterialColor.COLOR_LIGHT_BLUE)
        .put("COLOR_YELLOW", MaterialColor.COLOR_YELLOW)
        .put("COLOR_LIGHT_GREEN", MaterialColor.COLOR_LIGHT_GREEN)
        .put("COLOR_PINK", MaterialColor.COLOR_PINK)
        .put("COLOR_GRAY", MaterialColor.COLOR_GRAY)
        .put("COLOR_LIGHT_GRAY", MaterialColor.COLOR_LIGHT_GRAY)
        .put("COLOR_CYAN", MaterialColor.COLOR_CYAN)
        .put("COLOR_PURPLE", MaterialColor.COLOR_PURPLE)
        .put("COLOR_BLUE", MaterialColor.COLOR_BLUE)
        .put("COLOR_BROWN", MaterialColor.COLOR_BROWN)
        .put("COLOR_GREEN", MaterialColor.COLOR_GREEN)
        .put("COLOR_RED", MaterialColor.COLOR_RED)
        .put("COLOR_BLACK", MaterialColor.COLOR_BLACK)
        .put("GOLD", MaterialColor.GOLD)
        .put("DIAMOND", MaterialColor.DIAMOND)
        .put("LAPIS", MaterialColor.LAPIS)
        .put("EMERALD", MaterialColor.EMERALD)
        .put("PODZOL", MaterialColor.PODZOL)
        .put("NETHER", MaterialColor.NETHER)
        .put("TERRACOTTA_WHITE", MaterialColor.TERRACOTTA_WHITE)
        .put("TERRACOTTA_ORANGE", MaterialColor.TERRACOTTA_ORANGE)
        .put("TERRACOTTA_MAGENTA", MaterialColor.TERRACOTTA_MAGENTA)
        .put("TERRACOTTA_LIGHT_BLUE", MaterialColor.TERRACOTTA_LIGHT_BLUE)
        .put("TERRACOTTA_YELLOW", MaterialColor.TERRACOTTA_YELLOW)
        .put("TERRACOTTA_LIGHT_GREEN", MaterialColor.TERRACOTTA_LIGHT_GREEN)
        .put("TERRACOTTA_PINK", MaterialColor.TERRACOTTA_PINK)
        .put("TERRACOTTA_GRAY", MaterialColor.TERRACOTTA_GRAY)
        .put("TERRACOTTA_LIGHT_GRAY", MaterialColor.TERRACOTTA_LIGHT_GRAY)
        .put("TERRACOTTA_CYAN", MaterialColor.TERRACOTTA_CYAN)
        .put("TERRACOTTA_PURPLE", MaterialColor.TERRACOTTA_PURPLE)
        .put("TERRACOTTA_BLUE", MaterialColor.TERRACOTTA_BLUE)
        .put("TERRACOTTA_BROWN", MaterialColor.TERRACOTTA_BROWN)
        .put("TERRACOTTA_GREEN", MaterialColor.TERRACOTTA_GREEN)
        .put("TERRACOTTA_RED", MaterialColor.TERRACOTTA_RED)
        .put("TERRACOTTA_BLACK", MaterialColor.TERRACOTTA_BLACK)
        .put("CRIMSON_NYLIUM", MaterialColor.CRIMSON_NYLIUM)
        .put("CRIMSON_STEM", MaterialColor.CRIMSON_STEM)
        .put("CRIMSON_HYPHAE", MaterialColor.CRIMSON_HYPHAE)
        .put("WARPED_NYLIUM", MaterialColor.WARPED_NYLIUM)
        .put("WARPED_STEM", MaterialColor.WARPED_STEM)
        .put("WARPED_HYPHAE", MaterialColor.WARPED_HYPHAE)
        .put("WARPED_WART_BLOCK", MaterialColor.WARPED_WART_BLOCK)
        .build();

    /** A regular codec used for serializing material colors in config files. */
    public static Codec<MaterialColor> COLOR_CODEC = Codec.STRING.flatXmap(
        key -> getColor(key).map(DataResult::success).orElse(DataResult.error("No such color: " + key)),
        color -> serialize(color).map(DataResult::success).orElse(DataResult.error("Unknown color: " + color))
    );

    /** A regular codec used for serializing material names in config files. */
    public static Codec<Material> MATERIAL_CODEC = Codec.STRING.flatXmap(
        key -> getMaterial(key).map(DataResult::success).orElse(DataResult.error("No such material: " + key)),
        material -> serialize(material).map(DataResult::success).orElse(DataResult.error("Unknown material: " + material))
    );

    /** A regular codec used for serializing sound types in config files. */
    public static Codec<SoundType> SOUND_CODEC = Codec.STRING.flatXmap(
        key -> getSoundType(key).map(DataResult::success).orElse(DataResult.error("No such sound type: " + key)),
        sound -> serialize(sound).map(DataResult::success).orElse(DataResult.error("Unknown sound type: " + sound))
    );

    /**
     * Attempts to retrieve a material from the map.
     *
     * @param key The name of the material being queried.
     * @return The corresponding material, or else {@link Optional#empty}.
     */
    public static Optional<Material> getMaterial(final String key) {
        return nullable(MATERIAL_MAP.get(key.toUpperCase()));
    }

    /**
     * Returns every material name in the registry.
     *
     * @return All material keys.
     */
    public static Set<String> getMaterialNames() {
        return MATERIAL_MAP.keySet();
    }

    /**
     * Returns every material object in the registry.
     *
     * @return All known material values.
     */
    public static Set<Material> getMaterialValues() {
        return MATERIAL_MAP.values();
    }

    /**
     * Converts the input material to a string.
     *
     * @param value The actual {@link Material} object which may have a name.
     * @return The name of the material, or else {@link Optional#empty}.
     */
    public static Optional<String> serialize(final Material value) {
        return nullable(MATERIAL_MAP.inverse().get(value));
    }

    /**
     * Attempts to retrieve a sound type from the map.
     *
     * @param key The name of the sound type being queried.
     * @return The corresponding {@link SoundType}, or else {@link Optional#empty}.
     */
    public static Optional<SoundType> getSoundType(final String key) {
        return nullable(SOUND_MAP.get(key.toUpperCase()));
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
        return nullable(SOUND_MAP.inverse().get(value));
    }

    /**
     * Attempts to retrieve a material color from the map.
     *
     * @param key The name of the color being queried.
     * @return The actual color object, or else {@link Optional#empty}.
     */
    public static Optional<MaterialColor> getColor(final String key) {
        return nullable(COLOR_MAP.get(key.toUpperCase()));
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
    public static Set<MaterialColor> getColorValues() {
        return COLOR_MAP.values();
    }

    /**
     * Converts the input color type to a string.
     *
     * @param value The actual {@link MaterialColor} which may have a name.
     * @return The name of the color, or else {@link Optional#empty}.
     */
    public static Optional<String> serialize(final MaterialColor value) {
        return nullable(COLOR_MAP.inverse().get(value));
    }
}