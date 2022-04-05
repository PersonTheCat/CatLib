package personthecat.catlib.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.function.Consumer;

public class DynamicRegistries {

    public static final RegistryHandle<Biome> BIOMES =
        DynamicRegistryHandle.createHandle(Registry.BIOME_REGISTRY);

    public static final RegistryHandle<DimensionType> DIMENSION_TYPES =
        DynamicRegistryHandle.createHandle(Registry.DIMENSION_TYPE_REGISTRY);

    public static final RegistryHandle<ConfiguredFeature<?, ?>> CONFIGURED_FEATURES =
        DynamicRegistryHandle.createHandle(Registry.CONFIGURED_FEATURE_REGISTRY);

    public static final RegistryHandle<PlacedFeature> PLACED_FEATURES =
        DynamicRegistryHandle.createHandle(Registry.PLACED_FEATURE_REGISTRY);

    public static void updateRegistries(final RegistryAccess registries) {
        updateRegistry(BIOMES, registries, Registry.BIOME_REGISTRY);
        updateRegistry(DIMENSION_TYPES, registries, Registry.DIMENSION_TYPE_REGISTRY);
        updateRegistry(CONFIGURED_FEATURES, registries, Registry.CONFIGURED_FEATURE_REGISTRY);
        updateRegistry(PLACED_FEATURES, registries, Registry.PLACED_FEATURE_REGISTRY);
    }

    private static <T> void updateRegistry(final RegistryHandle<T> handle,
            final RegistryAccess registries, final ResourceKey<Registry<T>> key) {
        ((DynamicRegistryHandle<T>) handle).updateRegistry(new MojangRegistryHandle<>(registries.registryOrThrow(key)));
    }

    public static <T> Consumer<Consumer<RegistryHandle<T>>> listen(final RegistryHandle<T> handle, final Object mutex) {
        if (handle instanceof DynamicRegistryHandle) {
            return consumer -> ((DynamicRegistryHandle<T>) handle).listen(mutex, consumer);
        }
        return consumer -> {};
    }
}
