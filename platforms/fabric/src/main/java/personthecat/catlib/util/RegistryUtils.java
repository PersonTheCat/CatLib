package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.registry.MojangRegistryHandle;
import personthecat.catlib.event.registry.RegistryHandle;
import personthecat.catlib.exception.MissingElementException;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
@OverwriteClass
@InheritMissingMembers
public class RegistryUtils {

    private static final Map<Class<?>, RegistryHandle<?>> REGISTRY_BY_TYPE = new ConcurrentHashMap<>();

    @Inherit
    public static <T> RegistryHandle<T> getHandle(final ResourceKey<Registry<T>> key) {
        throw new MissingOverrideException();
    }

    @Overwrite
    @SuppressWarnings("unchecked")
    public static <T> Optional<RegistryHandle<T>> tryGetHandle(final ResourceKey<Registry<T>> key) {
        final Registry<T> registry = (Registry<T>) Registry.REGISTRY.get(key.location());
        if (registry != null) {
            return Optional.of(new MojangRegistryHandle<>(registry));
        }
        return Optional.ofNullable((Registry<T>) BuiltinRegistries.REGISTRY.get(key.location()))
            .map(MojangRegistryHandle::new);
    }

    @NotNull
    @Overwrite
    @SuppressWarnings("unchecked")
    public static <T> RegistryHandle<T> getByType(final Class<T> clazz) {
        return (RegistryHandle<T>) REGISTRY_BY_TYPE.computeIfAbsent(clazz, c -> {
            final RegistryHandle<?> builtin = findRegistry(BuiltinRegistries.REGISTRY, clazz);
            if (builtin != null) return builtin;
            final RegistryHandle<?> common = findRegistry(Registry.REGISTRY, clazz);
            if (common != null) return common;
            throw new MissingElementException("No registry for type: " + clazz.getSimpleName());
        });
    }

    @Nullable
    private static RegistryHandle<?> findRegistry(final Registry<? extends Registry<?>> registry, final Class<?> clazz) {
        for (final Registry<?> r : registry) {
            if (clazz.isInstance(r.iterator().next())) {
                return new MojangRegistryHandle<>(r);
            }
        }
        return null;
    }
}
