package personthecat.catlib.registry.forge;

import lombok.experimental.UtilityClass;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
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

@UtilityClass
public class RegistryUtilsImpl {

    private static final Map<Class<?>, RegistryHandle<?>> REGISTRY_BY_TYPE = new ConcurrentHashMap<>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> Optional<RegistryHandle<T>> tryGetHandle(final ResourceKey<? extends Registry<T>> key) {
        final IForgeRegistry<?> forgeRegistry = RegistryManager.ACTIVE.getRegistry(key.location());
        if (forgeRegistry != null) {
            return Optional.of(new ForgeRegistryHandle<>((IForgeRegistry) forgeRegistry));
        }
        final Registry<T> builtinRegistry = (Registry<T>) BuiltInRegistries.REGISTRY.get(key.location());
        if (builtinRegistry != null) {
            return Optional.of(new MojangRegistryHandle<>(builtinRegistry));
        }
        return Optional.empty();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> Optional<RegistryHandle<T>> tryGetByType(final Class<T> clazz) {
        return Optional.ofNullable((RegistryHandle<T>) REGISTRY_BY_TYPE.computeIfAbsent(clazz, c -> {
            final RegistryHandle<?> forge = findForge(clazz);
            if (forge != null) return forge;
            final RegistryHandle<?> mojang = findMojang(clazz);
            return mojang != null ? mojang : findCreateDynamic(clazz);
        }));
    }

    @Nullable
    private static RegistryHandle<?> findForge(final Class<?> clazz) {
        for (final IForgeRegistry<?> r : RegistryManager.ACTIVE.getRegistries().values()) {
            if (clazz.isInstance(r.iterator().next())) {
                return new ForgeRegistryHandle<>(r);
            }
        }
        return null;
    }

    @Nullable
    private static RegistryHandle<?> findMojang(final Class<?> clazz) {
        for (final Registry<?> r : BuiltInRegistries.REGISTRY) {
            if (clazz.isInstance(r.iterator().next())) {
                return new MojangRegistryHandle<>(r);
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RegistryHandle<?> findCreateDynamic(final Class<?> clazz) {
        for (final Field f : clazz.getFields()) {
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
