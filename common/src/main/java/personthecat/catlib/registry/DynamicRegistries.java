package personthecat.catlib.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.exception.MissingElementException;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DynamicRegistries {

    private static final RegistryHandle<RegistryHandle<?>> REGISTRY =
        RegistryHandle.create(ResourceKey.createRegistryKey(Registries.ROOT_REGISTRY_NAME));

    public static final RegistryHandle<Biome> BIOME = createAndRegister(Registries.BIOME);
    public static final RegistryHandle<DimensionType> DIMENSION_TYPE = createAndRegister(Registries.DIMENSION_TYPE);
    public static final RegistryHandle<ConfiguredWorldCarver<?>> CONFIGURED_CARVER = createAndRegister(Registries.CONFIGURED_CARVER);
    public static final RegistryHandle<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE = createAndRegister(Registries.CONFIGURED_FEATURE);
    public static final RegistryHandle<PlacedFeature> PLACED_FEATURE = createAndRegister(Registries.PLACED_FEATURE);

    static { // autoload any we don't have static references to
        BuiltInRegistries.REGISTRY.keySet()
            .stream()
            .filter(Predicate.not(REGISTRY::isRegistered))
            .map(ResourceKey::createRegistryKey)
            .forEach(DynamicRegistries::createAndRegister);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void updateRegistries(final RegistryAccess registries) {
        registries.registries().forEach(entry -> {
            final ResourceKey<? extends Registry<?>> key = entry.key();
            RegistryHandle<?> handle = REGISTRY.lookup(key.location());
            if (handle == null) {
                handle = createAndRegister((ResourceKey) key);
            }
            updateRegistry(handle, (Registry) entry.value());
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onSeverClosed() {
        REGISTRY.stream().forEach(handle ->
            ((DynamicRegistryHandle) handle).updateRegistry(new DummyRegistryHandle<>(handle.key())));
    }

    private static <T> RegistryHandle<T> createAndRegister(final ResourceKey<? extends Registry<T>> key) {
        final RegistryHandle<T> handle = DynamicRegistryHandle.createHandle(key);
        REGISTRY.register(key.location(), handle);
        return handle;
    }

    private static <T> void updateRegistry(final RegistryHandle<T> handle, final Registry<T> registry) {
        ((DynamicRegistryHandle<T>) handle).updateRegistry(new MojangRegistryHandle<>(registry));
    }

    public static ResourceKey<? extends Registry<RegistryHandle<?>>> rootKey() {
        return REGISTRY.key();
    }

    @SuppressWarnings("unchecked")
    public static <T> RegistryHandle<T> get(final ResourceKey<? extends Registry<T>> key) {
        return (RegistryHandle<T>) ((rootKey().equals(key)) ? REGISTRY : REGISTRY.lookup(key.location()));
    }

    public static <T> RegistryHandle<T> getOrCreate(final ResourceKey<? extends Registry<T>> key) {
        return Optional.ofNullable(get(key)).orElseGet(() -> createAndRegister(key));
    }

    public static <T> RegistryHandle<T> getOrThrow(final ResourceKey<? extends Registry<T>> key) {
        return Optional.ofNullable(get(key)).orElseThrow(() ->
            new MissingElementException("Registry unknown or not ready: " + key.location()));
    }

    public static <T> @Nullable RegistryHandle<T> lookup(final Class<T> type) {
        return RegistryUtils.tryGetByType(type).map(handle -> get(handle.key())).orElse(null);
    }

    public static <T> RegistryHandle<T> getByType(final Class<T> type) {
        return get(RegistryUtils.getByType(type).key());
    }

    public static <T> Consumer<Consumer<RegistryHandle<T>>> listen(final RegistryHandle<T> handle, final Object mutex) {
        if (handle instanceof DynamicRegistryHandle) {
            return consumer -> ((DynamicRegistryHandle<T>) handle).listen(mutex, consumer);
        }
        return consumer -> {};
    }
}
