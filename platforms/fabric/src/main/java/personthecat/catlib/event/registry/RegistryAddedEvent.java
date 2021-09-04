package personthecat.catlib.event.registry;

import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import personthecat.catlib.event.LibEvent;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@OverwriteClass
public class RegistryAddedEvent {

    private static final Map<ResourceKey<?>, LibEvent<RegistryAddedCallback<?>>> EVENT_MAP = new ConcurrentHashMap<>();

    @Overwrite
    @SuppressWarnings("unchecked")
    public static <T> LibEvent<RegistryAddedCallback<T>> get(final ResourceKey<Registry<T>> key) {
        return (LibEvent<RegistryAddedCallback<T>>) (Object) EVENT_MAP.computeIfAbsent(key, k ->
            (LibEvent<RegistryAddedCallback<?>>) (Object) create(key));
    }

    private static <T> LibEvent<RegistryAddedCallback<T>> create(final ResourceKey<Registry<T>> key) {
        final MojangRegistryHandle<T> handle = newHandle(key);
        final LibEvent<RegistryAddedCallback<T>> event = newEvent();
        RegistryEntryAddedCallback.event(handle.getRegistry()).register((i, id, t) ->
            event.invoker().onRegistryAdded(handle, id, t));
        return event;
    }

    @SuppressWarnings("unchecked")
    private static <T> MojangRegistryHandle<T> newHandle(final ResourceKey<Registry<T>> key) {
        Registry<T> registry = (Registry<T>) Registry.REGISTRY.get(key.location());
        if (registry == null) registry = (Registry<T>) BuiltinRegistries.REGISTRY.get(key.location());
        Objects.requireNonNull(registry, "No registry for key: " + key);
        return new MojangRegistryHandle<>(registry);
    }

    private static <T> LibEvent<RegistryAddedCallback<T>> newEvent() {
        return LibEvent.create(callbacks -> (h, id, t) -> callbacks.forEach(c -> c.onRegistryAdded(h, id, t)));
    }
}
