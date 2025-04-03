package personthecat.catlib.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.FloatProviderType;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.material.Fluid;

@SuppressWarnings("unused") // not going to test all
public class CommonRegistries {
    public static final RegistryHandle<Block> BLOCK = RegistryUtils.getHandle(Registries.BLOCK);
    public static final RegistryHandle<Fluid> FLUID = RegistryUtils.getHandle(Registries.FLUID);
    public static final RegistryHandle<Item> ITEM = RegistryUtils.getHandle(Registries.ITEM);
    public static final RegistryHandle<EntityType<?>> ENTITY = RegistryUtils.getHandle(Registries.ENTITY_TYPE);
    public static final RegistryHandle<MapCodec<? extends BiomeSource>> BIOME_SOURCE = RegistryUtils.getHandle(Registries.BIOME_SOURCE);
    public static final RegistryHandle<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATOR = RegistryUtils.getHandle(Registries.CHUNK_GENERATOR);
    public static final RegistryHandle<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPE = RegistryUtils.getHandle(Registries.DENSITY_FUNCTION_TYPE);
    public static final RegistryHandle<WorldCarver<?>> CARVER = RegistryUtils.getHandle(Registries.CARVER);
    public static final RegistryHandle<Feature<?>> FEATURE = RegistryUtils.getHandle(Registries.FEATURE);
    public static final RegistryHandle<BlockStateProviderType<?>> BLOCK_STATE_PROVIDER_TYPE = RegistryUtils.getHandle(Registries.BLOCK_STATE_PROVIDER_TYPE);
    public static final RegistryHandle<IntProviderType<?>> INT_PROVIDER_TYPE = RegistryUtils.getHandle(Registries.INT_PROVIDER_TYPE);
    public static final RegistryHandle<FloatProviderType<?>> FLOAT_PROVIDER_TYPE = RegistryUtils.getHandle(Registries.FLOAT_PROVIDER_TYPE);
    public static final RegistryHandle<HeightProviderType<?>> HEIGHT_PROVIDER_TYPE = RegistryUtils.getHandle(Registries.HEIGHT_PROVIDER_TYPE);
    public static final RegistryHandle<StructurePlacementType<?>> STRUCTURE_PLACEMENT = RegistryUtils.getHandle(Registries.STRUCTURE_PLACEMENT);
    public static final RegistryHandle<StructurePieceType> STRUCTURE_PIECE = RegistryUtils.getHandle(Registries.STRUCTURE_PIECE);
    public static final RegistryHandle<StructureType<?>> STRUCTURE_TYPE = RegistryUtils.getHandle(Registries.STRUCTURE_TYPE);
    public static final RegistryHandle<StructureProcessorType<?>> STRUCTURE_PROCESSOR = RegistryUtils.getHandle(Registries.STRUCTURE_PROCESSOR);
    public static final RegistryHandle<PlacementModifierType<?>> PLACEMENT_MODIFIER_TYPE = RegistryUtils.getHandle(Registries.PLACEMENT_MODIFIER_TYPE);
    public static final RegistryHandle<RuleTestType<?>> RULE_TEST_TYPE = RegistryUtils.getHandle(Registries.RULE_TEST);

    public static <T> RegistryHandle<T> get(final ResourceKey<Registry<T>> key) {
        return RegistryUtils.getHandle(key);
    }
}
