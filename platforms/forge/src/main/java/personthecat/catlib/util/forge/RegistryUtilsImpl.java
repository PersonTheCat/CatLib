package personthecat.catlib.util.forge;

import lombok.experimental.UtilityClass;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.registry.ForgeRegistryHandle;
import personthecat.catlib.event.registry.MojangRegistryHandle;
import personthecat.catlib.event.registry.RegistryHandle;
import personthecat.catlib.exception.MissingElementException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class RegistryUtilsImpl {

    private static final Map<Class<?>, RegistryHandle<?>> REGISTRY_BY_TYPE = new ConcurrentHashMap<>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> Optional<RegistryHandle<T>> tryGetHandle(final ResourceKey<Registry<T>> key) {
        final IForgeRegistry<?> forgeRegistry = RegistryManager.ACTIVE.getRegistry(key.location());
        if (forgeRegistry != null) {
            return Optional.of(new ForgeRegistryHandle<>((ForgeRegistry) forgeRegistry));
        }
        final Registry<T> mojangRegistry = (Registry<T>) Registry.REGISTRY.get(key.location());
        if (mojangRegistry != null) {
            return Optional.of(new MojangRegistryHandle<>(mojangRegistry));
        }
        final Registry<T> builtinRegistry = (Registry<T>) BuiltinRegistries.REGISTRY.get(key.location());
        if (builtinRegistry != null) {
            return Optional.of(new MojangRegistryHandle<>(builtinRegistry));
        }
        return Optional.empty();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> RegistryHandle<T> getByType(final Class<T> clazz) {
        return (RegistryHandle<T>) REGISTRY_BY_TYPE.computeIfAbsent(clazz, c -> {
            final RegistryHandle<?> forge = findForge(clazz);
            if (forge != null) return forge;
            final RegistryHandle<?> builtin = findRegistry(BuiltinRegistries.REGISTRY, clazz);
            if (builtin != null) return builtin;
            final RegistryHandle<?> common = findRegistry(Registry.REGISTRY, clazz);
            if (common != null) return common;
            throw new MissingElementException("No registry for type: " + clazz.getSimpleName());
        });
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RegistryHandle<?> findForge(final Class<?> clazz) {
        // Deprecation: We'll have to manually implement this behavior to support annotations in 1.19.
        if (clazz == Biome.class) return new ForgeRegistryHandle<>((ForgeRegistry<?>) ForgeRegistries.BIOMES);
        final ForgeRegistry<?> registry = (ForgeRegistry<?>) RegistryManager.ACTIVE.getRegistry((Class) clazz);
        return registry != null ? new ForgeRegistryHandle<>(registry) : null;
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
