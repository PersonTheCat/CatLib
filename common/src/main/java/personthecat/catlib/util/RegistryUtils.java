package personthecat.catlib.util;

import com.mojang.serialization.Lifecycle;
import lombok.experimental.UtilityClass;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import personthecat.catlib.event.registry.RegistryHandle;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.catlib.exception.RegistryLookupException;
import personthecat.overwritevalidator.annotations.OverwriteTarget;
import personthecat.overwritevalidator.annotations.PlatformMustOverwrite;

import java.util.Optional;

@UtilityClass
@OverwriteTarget(required = true)
public class RegistryUtils {

    /**
     * Acquires a handle on the registry corresponding to this key. In particular, this
     * is useful for two reason:
     *
     * <ul>
     *   <li>To dynamically acquire a registry from either {@link Registry} or {@link BuiltinRegistries}</li>
     *   <li>On Forge, to acquire either of the above or the preferred Forge registry.</li>
     * </ul>
     *
     * @throws RegistryLookupException If the expected registry does not exist.
     * @param key The key of the registry being returned.
     * @param <T> The type of object contained within the registry.
     * @return A platform-agnostic representation of this registry.
     */
    public static <T> RegistryHandle<T> getHandle(final ResourceKey<Registry<T>> key) {
        return tryGetHandle(key).orElseThrow(() -> new RegistryLookupException(key));
    }

    /**
     * Attempts to acquire a handle on the corresponding registry for this key. Or else,
     * returns {@link Optional#empty}.
     *
     * @param key The key of the registry being returned.
     * @param <T> The type of object contained within the registry.
     * @return A platform-agnostic representation of this registry, or else {@link Optional#empty}.
     */
    @PlatformMustOverwrite
    public static <T> Optional<RegistryHandle<T>> tryGetHandle(final ResourceKey<Registry<T>> key) {
        throw new MissingOverrideException();
    }

    /**
     * Inserts a registry of this type into {@link BuiltinRegistries#REGISTRY}.
     *
     * @param key The key of the registry being added.
     * @param <T> The type of object contained within the registry.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> void createBuiltinRegistry(final ResourceKey<Registry<T>> key) {
        if (!BuiltinRegistries.REGISTRY.containsKey(key.location())) {
            final Registry<T> dummyRegistry = new MappedRegistry<>(key, Lifecycle.experimental());
            Registry.register((Registry) BuiltinRegistries.REGISTRY, key.location(), dummyRegistry);
        }
    }
}
