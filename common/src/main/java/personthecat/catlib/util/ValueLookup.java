package personthecat.catlib.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import lombok.experimental.UtilityClass;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import personthecat.overwritevalidator.annotations.OverwriteTarget;

import java.util.Optional;

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
        .put("SNOW", Material.TOP_SNOW)
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

    /** Attempts to retrieve a material from the map. */
    public static Optional<Material> getMaterial(final String key) {
        return nullable(MATERIAL_MAP.get(key.toUpperCase()));
    }

    /** Attempts to retrieve a sound type from the map. */
    public static Optional<SoundType> getSoundType(final String key) {
        return nullable(SOUND_MAP.get(key.toUpperCase()));
    }

    /** Converts the input material to a string. */
    public static Optional<String> serialize(final Material value) {
        return nullable(MATERIAL_MAP.inverse().get(value));
    }

    /** Converts the input sound type to a string. */
    public static Optional<String> serialize(final SoundType value) {
        return nullable(SOUND_MAP.inverse().get(value));
    }
}