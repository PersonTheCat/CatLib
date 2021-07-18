package personthecat.catlib.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import lombok.experimental.UtilityClass;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.Map;
import java.util.Optional;

import static personthecat.catlib.util.Shorthand.full;
import static personthecat.catlib.util.Shorthand.nullable;

@UtilityClass
@OverwriteClass
@SuppressWarnings("unused")
public class ValueLookup {

    /** A map of MCP material names which don't overlap with the originals. */
    private static final Map<String, Material> MCP_MATERIAL_MAP = ImmutableMap.<String, Material>builder()
        .put("STRUCTURE_VOID", Material.STRUCTURAL_AIR)
        .put("CARPET", Material.CLOTH_DECORATION)
        .put("PLANTS", Material.PLANT)
        .put("OCEAN_PLANTS", Material.WATER_PLANT)
        .put("TALL_PLANTS", Material.REPLACEABLE_PLANT)
        .put("NETHER_PLANTS", Material.REPLACEABLE_FIREPROOF_PLANT)
        .put("SEA_GRASS", Material.REPLACEABLE_WATER_PLANT)
        .put("SNOW", Material.TOP_SNOW)
        .put("MISCELLANEOUS", Material.DECORATION)
        .put("REDSTONE_LIGHT", Material.BUILDABLE_GLASS)
        .put("EARTH", Material.DIRT)
        .put("ORGANIC", Material.GRASS)
        .put("PACKED_ICE", Material.ICE_SOLID)
        .put("SHULKER", Material.SHULKER_SHELL)
        .put("TNT", Material.EXPLOSIVE)
        .put("ROCK", Material.STONE)
        .put("IRON", Material.METAL)
        .put("SNOW_BLOCK", Material.SNOW)
        .put("ANVIL", Material.HEAVY_METAL)
        .put("GOURD", Material.VEGETABLE)
        .put("DRAGON_EGG", Material.EGG)
        .build();


    /** A map of MCP material names which don't overlap with the originals. */
    private static final Map<String, SoundType> MCP_SOUND_MAP = ImmutableMap.<String, SoundType>builder()
        .put("GROUND", SoundType.GRAVEL)
        .put("PLANT", SoundType.GRASS)
        .put("LILY_PADS", SoundType.LILY_PAD)
        .put("CLOTH", SoundType.WOOL)
        .put("SLIME", SoundType.SLIME_BLOCK)
        .put("HONEY", SoundType.HONEY_BLOCK)
        .put("CORAL", SoundType.CORAL_BLOCK)
        .put("STEM", SoundType.HARD_CROP)
        .put("HYPHAE", SoundType.STEM)
        .put("ROOT", SoundType.ROOTS)
        .put("NETHER_VINE", SoundType.WEEPING_VINES)
        .put("NETHER_VINE_LOWER_PITCH", SoundType.TWISTING_VINES)
        .put("NETHER_BRICK", SoundType.NETHER_BRICKS)
        .put("NETHER_SPROUT", SoundType.NETHER_SPROUTS)
        .put("BONE", SoundType.BONE_BLOCK)
        .put("NETHERITE", SoundType.NETHERITE_BLOCK)
        .put("NETHER_GOLD", SoundType.NETHER_GOLD_ORE)
        .build();

    @Inherit
    private static final BiMap<String, Material> MATERIAL_MAP = ImmutableBiMap.<String, Material>builder().build();

    @Inherit
    private static final BiMap<String, SoundType> SOUND_MAP = ImmutableBiMap.<String, SoundType>builder().build();

    @Overwrite
    public static Optional<Material> getMaterial(final String key) {
        final String caps = key.toUpperCase();
        final Material moj = MATERIAL_MAP.get(caps);
        return moj != null ? full(moj) : nullable(MCP_MATERIAL_MAP.get(caps));
    }

    @Overwrite
    public static Optional<SoundType> getSoundType(final String key) {
        final String caps = key.toUpperCase();
        final SoundType moj = SOUND_MAP.get(caps);
        return moj != null ? full(moj) : nullable(MCP_SOUND_MAP.get(caps));
    }

    @Inherit
    public static Optional<String> serialize(final Material value) {
        throw new MissingOverrideException();
    }

    @Inherit
    public static Optional<String> serialize(final SoundType value) {
        throw new MissingOverrideException();
    }
}
