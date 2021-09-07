package personthecat.catlib.event.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

public class DynamicRegistries {
    public static final RegistryHandle<Biome> BIOMES = DynamicRegistryHandle.createHandle(Registry.BIOME_REGISTRY);
    public static final RegistryHandle<DimensionType> DIMENSION_TYPES = DynamicRegistryHandle.createHandle(Registry.DIMENSION_TYPE_REGISTRY);

    @SuppressWarnings("unchecked")
    public static void updateRegistries(final RegistryAccess registries) {
        ((DynamicRegistryHandle<Biome>) BIOMES).updateRegistry(new MojangRegistryHandle<>(registries.registryOrThrow(Registry.BIOME_REGISTRY)));
        ((DynamicRegistryHandle<DimensionType>) DIMENSION_TYPES).updateRegistry(new MojangRegistryHandle<>(registries.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)));
    }
}
