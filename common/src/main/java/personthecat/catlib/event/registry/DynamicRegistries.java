package personthecat.catlib.event.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class DynamicRegistries {

    public static final RegistryHandle<Biome> BIOMES =
        DynamicRegistryHandle.createHandle(Registry.BIOME_REGISTRY);

    public static final RegistryHandle<DimensionType> DIMENSION_TYPES =
        DynamicRegistryHandle.createHandle(Registry.DIMENSION_TYPE_REGISTRY);

    public static final RegistryHandle<ConfiguredFeature<?, ?>> CONFIGURED_FEATURES =
        DynamicRegistryHandle.createHandle(Registry.CONFIGURED_FEATURE_REGISTRY);

    public static void updateRegistries(final RegistryAccess registries) {
        updateRegistry(BIOMES, registries, Registry.BIOME_REGISTRY);
        updateRegistry(DIMENSION_TYPES, registries, Registry.DIMENSION_TYPE_REGISTRY);
        updateRegistry(CONFIGURED_FEATURES, registries, Registry.CONFIGURED_FEATURE_REGISTRY);
    }

    private static <T> void updateRegistry(final RegistryHandle<T> handle,
            final RegistryAccess registries, final ResourceKey<Registry<T>> key) {
        ((DynamicRegistryHandle<T>) handle).updateRegistry(new MojangRegistryHandle<>(registries.registryOrThrow(key)));
    }
}
