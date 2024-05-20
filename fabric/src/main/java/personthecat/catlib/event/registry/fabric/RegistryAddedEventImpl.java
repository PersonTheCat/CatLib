package personthecat.catlib.event.registry.fabric;

import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import personthecat.catlib.event.LibEvent;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.event.registry.RegistryAddedCallback;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.registry.RegistryUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryAddedEventImpl {
    private static final Map<ResourceKey<?>, LibEvent<RegistryAddedCallback<?>>> EVENT_MAP = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<?>, LibEvent<RegistryAddedCallback<?>>> DYNAMIC_EVENT_MAP = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> LibEvent<RegistryAddedCallback<T>> get(final ResourceKey<Registry<T>> key) {
        return (LibEvent<RegistryAddedCallback<T>>) (Object) EVENT_MAP.computeIfAbsent(key, k ->
            (LibEvent<RegistryAddedCallback<?>>) (Object) create(key));
    }

    public static <T> void withRetroactive(final ResourceKey<Registry<T>> key, final RegistryAddedCallback<T> f) {
        get(key).register(f);
        runRetroactively(key, f);
    }

    public static <T> void withDynamic(final ResourceKey<Registry<T>> key, final RegistryAddedCallback<T> f) {
        get(key).register(f);
        runDynamically(key, f);
    }

    public static <T> void exhaustive(final ResourceKey<Registry<T>> key, final RegistryAddedCallback<T> f) {
        get(key).register(f);
        runRetroactively(key, f);
        runDynamically(key, f);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onRegistryAccess(final RegistryAccess registries) {
        DYNAMIC_EVENT_MAP.forEach((key, event) -> {
            final RegistryHandle<Object> registry = (RegistryHandle<Object>) registries.registryOrThrow((ResourceKey) key);
            registry.forEach((id, t) -> ((LibEvent<RegistryAddedCallback<Object>>) (Object) event).invoker()
                .onRegistryAdded(registry, id, t));
        });
    }

    private static <T> LibEvent<RegistryAddedCallback<T>> create(final ResourceKey<Registry<T>> key) {
        final RegistryHandle<T> handle = RegistryUtils.getHandle(key);
        final LibEvent<RegistryAddedCallback<T>> event = newEvent();
        RegistryEntryAddedCallback.event(((MojangRegistryHandle<T>) handle).getRegistry()).register((i, id, t) ->
            event.invoker().onRegistryAdded(handle, id, t));
        return event;
    }

    private static <T> LibEvent<RegistryAddedCallback<T>> newEvent() {
        return LibEvent.nonRecursive(callbacks -> (h, id, t) -> callbacks.forEach(c -> c.onRegistryAdded(h, id, t)));
    }

    private static <T> void runRetroactively(final ResourceKey<Registry<T>> key, final RegistryAddedCallback<T> f) {
        final RegistryHandle<T> handle = RegistryUtils.getHandle(key);
        handle.forEach((id, t) -> f.onRegistryAdded(handle, id, t));
    }

    @SuppressWarnings("unchecked")
    private static <T> void runDynamically(final ResourceKey<Registry<T>> key, final RegistryAddedCallback<T> f) {
        DYNAMIC_EVENT_MAP.computeIfAbsent(key, k -> (LibEvent<RegistryAddedCallback<?>>) (Object) newEvent()).register(f);
    }
}
