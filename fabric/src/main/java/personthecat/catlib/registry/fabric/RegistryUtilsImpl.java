package personthecat.catlib.registry.fabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.registry.RegistryHandle;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RegistryUtilsImpl {

    private static final Map<Class<?>, RegistryHandle<?>> REGISTRY_BY_TYPE = new ConcurrentHashMap<>();

    private RegistryUtilsImpl() {}

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
    @SuppressWarnings({"rawtypes","unchecked"})
    private static RegistryHandle<?> findRegistry(final Class<?> clazz) {
        for (final Registry<?> r : BuiltInRegistries.REGISTRY) {
            final var itr = r.iterator();
            if (itr.hasNext() && clazz.isInstance(itr.next())) {
                return new MojangRegistryHandle<>(r);
            }
        }
        for (final Field f : Registries.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && clazz.equals(resolveRegistryType(f))) {
                try {
                    final var key = (ResourceKey) f.get(null);
                    return DynamicRegistries.getOrCreate(key);
                } catch (final ReflectiveOperationException ignored) {}
            }
        }
        return null;
    }

    @Nullable
    private static Class<?> resolveRegistryType(final Field f) {
        if (ResourceKey.class.equals(f.getType())
                && f.getGenericType() instanceof ParameterizedType p1) {
            final var p1Args = p1.getActualTypeArguments();
            if (p1Args.length > 0
                    && p1Args[0] instanceof ParameterizedType p2
                    && Registry.class.equals(p2.getRawType())) {
                final var p2Args = p2.getActualTypeArguments();
                if (p2Args.length > 0 && p2Args[0] instanceof Class<?> c) {
                    return c;
                }
            }
        }
        return null;
    }
}
