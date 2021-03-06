package personthecat.catlib.event.registry;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;
import personthecat.catlib.event.LibEvent;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.catlib.exception.RegistryLookupException;
import personthecat.catlib.registry.RegistryHandle;

public class RegistryAddedEvent {

    /**
     * Exposes the currently-available {@link RegistryAddedEvent} for a registry of the given type.
     * This event will fire any time an object is registered to this registry.
     *
     * <p>Note that on the Forge platform, this event exposes a {@link RegistryHandle} which can
     * either be a {@link Registry Mojang registry} or a Forge registry. The registry handle does
     * allow new elements to be registered, which is guaranteed to succeed on the Forge platform
     * due to when this event is fired.
     *
     * <p>Also note that the majority of objects in this registry have likely already been registered.
     * For this reason, you may prefer to leverage the {@link #withRetroactive} method, which will
     * fire immediately for each object already in the registry.
     *
     * <p>Finally, you should be aware that this event will fire for a single, builtin registry only.
     * This means that dynamic registries will not be covered. If you wish to register additional
     * callbacks to the dynamic registries, use {@link #withDynamic} instead.
     *
     * @throws RegistryLookupException If no registry is found.
     * @param key The key of the registry which this event corresponds to.
     * @param <T> The type of object in the registry.
     * @return A callback registrar for this registry.
     */
    @ExpectPlatform
    public static <T> LibEvent<RegistryAddedCallback<T>> get(final ResourceKey<Registry<T>> key) {
        throw new MissingOverrideException();
    }

    /**
     * This method registers a {@link RegistryAddedCallback} for a registry of the given type.
     * Unlike {@link #get}, <b>this method will also fire the callback for every object already
     * contained within the registry</b>.
     *
     * <p>On the Forge platform, this means the registry will be forcibly unfrozen temporarily.
     * Realistically speaking, this is a safe operation unless the Forge registry events have
     * already fired. In that case, additional registry entries will simply have no effect.
     *
     * <p>Note that dynamic registries are not covered by this event.
     *
     * @throws RegistryLookupException If no registry is found.
     * @param key The key of the registry which this event corresponds to.
     * @param f   The callback to fire in for every element in the registry.
     * @param <T> The type of object in the registry.
     */
    @ExpectPlatform
    public static <T> void withRetroactive(final ResourceKey<Registry<T>> key, final RegistryAddedCallback<T> f) {
        throw new MissingOverrideException();
    }

    /**
     * This method registers a {@link RegistryAddedCallback} for a registry of the given type.
     * Unlike {@link #get}, <b>this method will also fire the callback for every object in a
     * corresponding dynamic registry of the given type.</b> It will fire <b>every time the
     * dynamic registries are loaded</b>.
     *
     * <p>Note that any objects already contained within the registry will not be covered by
     * this event.
     *
     * @throws RegistryLookupException If no active registry is found.
     * @throws IllegalStateException If a dynamic registry is not found on {@link RegistryAccessEvent}.
     * @param key The key of the registry which this event corresponds to.
     * @param f   The callback to fire in for every element in the registry.
     * @param <T> The type of object in the registry.
     */
    @ExpectPlatform
    public static <T> void withDynamic(final ResourceKey<Registry<T>> key, final RegistryAddedCallback<T> f) {
        throw new MissingOverrideException();
    }

    /**
     * This method registers a {@link RegistryAddedCallback} for a registry of the given type.
     * Unlike {@link #get}, {@link #withRetroactive}, and {@link #withDynamic}, <b>this callback
     * will fire all objects already in the registry and all objects in the dynamic registries.</b>
     *
     * @throws RegistryLookupException If no active registry is found.
     * @throws IllegalStateException If a dynamic registry is not found on {@link RegistryAccessEvent}.
     * @param key The key of the registry which this event corresponds to.
     * @param f   The callback to fire in for every element in the registry.
     * @param <T> The type of object in the registry.
     */
    @ExpectPlatform
    public static <T> void exhaustive(final ResourceKey<Registry<T>> key, final RegistryAddedCallback<T> f) {
        throw new MissingOverrideException();
    }

    /**
     * Runs any platform-specific setup required on {@link RegistryAccessEvent}.
     *
     * @param access The dynamic registries after data packs are loaded.
     */
    @ExpectPlatform
    @ApiStatus.Internal
    public static void onRegistryAccess(final RegistryAccess access) {
        throw new MissingOverrideException();
    }
}
