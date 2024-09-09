package personthecat.catlib.registry;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.MissingElementException;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.catlib.exception.RegistryLookupException;

import java.util.Optional;

public final class RegistryUtils {

    private RegistryUtils() {}

    /**
     * Acquires a handle on the registry corresponding to this key. In particular, this
     * is useful for two reason:
     *
     * <ul>
     *   <li>To dynamically acquire a registry from either {@link Registry} or {@link BuiltInRegistries}</li>
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
    @ExpectPlatform
    public static <T> Optional<RegistryHandle<T>> tryGetHandle(final ResourceKey<? extends Registry<T>> key) {
        throw new MissingOverrideException();
    }

    /**
     * Acquires a handle on a registry when given the element type. For example, when
     * given <code>BiomeSource.class</code>, will return {@link BuiltInRegistries#BIOME_SOURCE}.
     * On the Forge platform, this method will return the equivalent Forge registry.
     *
     * @throws MissingElementException if the expected registry is not found.
     * @param clazz The element type contained within the registry.
     * @param <T>   The type token of this element.
     * @return A handle on the expected registry, guaranteed.
     */
    public static <T> RegistryHandle<T> getByType(final Class<T> clazz) {
        return tryGetByType(clazz).orElseThrow(() ->
            new MissingElementException("No registry for type: " + clazz.getSimpleName()));
    }

    /**
     * Variant of {@link #getByType} which simply returns {@link Optional#empty empty} if a
     * registry is not found for the given type.
     *
     * @param clazz The element type contained within the registry.
     * @param <T>   The type token of this element.
     * @return A handle on the expected registry, or else {@link Optional#empty}.
     */
    @NotNull
    @ExpectPlatform
    public static <T> Optional<RegistryHandle<T>> tryGetByType(final Class<T> clazz) {
        throw new MissingOverrideException();
    }
}
