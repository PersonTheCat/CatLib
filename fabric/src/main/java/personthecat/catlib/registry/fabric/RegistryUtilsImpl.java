package personthecat.catlib.registry.fabric;

import lombok.experimental.UtilityClass;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.registry.RegistryHandle;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
@SuppressWarnings("unused")
public class RegistryUtilsImpl {

    private static final Map<Class<?>, RegistryHandle<?>> REGISTRY_BY_TYPE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> Optional<RegistryHandle<T>> tryGetHandle(final ResourceKey<? extends Registry<T>> key) {
        final Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(key.location());
        if (registry != null) {
            return Optional.of(new MojangRegistryHandle<>(registry));
        }
        return Optional.ofNullable((Registry<T>) BuiltInRegistries.REGISTRY.get(key.location()))
            .map(MojangRegistryHandle::new);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> Optional<RegistryHandle<T>> tryGetByType(final Class<T> clazz) {
        return Optional.ofNullable((RegistryHandle<T>) REGISTRY_BY_TYPE
            .computeIfAbsent(clazz, c -> findRegistry(clazz)));
    }

    @Nullable
    private static RegistryHandle<?> findRegistry(final Class<?> clazz) {
        for (final Registry<?> r : BuiltInRegistries.REGISTRY) {
            if (clazz.isInstance(r.iterator().next())) {
                return new MojangRegistryHandle<>(r);
            }
        }
        return null;
    }
}
