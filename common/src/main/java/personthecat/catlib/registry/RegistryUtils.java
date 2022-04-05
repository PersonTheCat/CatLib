package personthecat.catlib.registry;

import com.mojang.serialization.Lifecycle;
import dev.architectury.injectables.annotations.ExpectPlatform;
import lombok.experimental.UtilityClass;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.MissingElementException;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.catlib.exception.RegistryLookupException;

import java.util.Optional;

@UtilityClass
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
    @ExpectPlatform
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
            final Registry<T> dummyRegistry = new MappedRegistry<>(key, Lifecycle.experimental(), null);
            Registry.register((Registry) BuiltinRegistries.REGISTRY, key.location(), dummyRegistry);
        }
    }

    /**
     * Acquires a handle on a registry when given the element type. For example, when
     * given <code>Biome.class</code>, will return {@link BuiltinRegistries#BIOME}.
     * On the Forge platform, this method will return the equivalent Forge registry.
     *
     * @throws MissingElementException if the expected registry is not found.
     * @param clazz The element type contained within the registry.
     * @param <T>   The type token of this element.
     * @return A handle on the expected registry, guaranteed.
     */
    @NotNull
    @ExpectPlatform
    public static <T> RegistryHandle<T> getByType(final Class<T> clazz) {
        throw new MissingOverrideException();
    }
}
