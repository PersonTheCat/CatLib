package personthecat.catlib.registry.forge;

import lombok.experimental.UtilityClass;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.registry.RegistryHandle;

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
            return forge != null ? forge : findMojang(clazz);
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
}
