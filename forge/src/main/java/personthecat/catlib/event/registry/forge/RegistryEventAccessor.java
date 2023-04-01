package personthecat.catlib.event.registry.forge;

import personthecat.catlib.event.LibEvent;
import personthecat.catlib.event.registry.RegistryAddedCallback;

public interface RegistryEventAccessor<T> {
    LibEvent<RegistryAddedCallback<T>> getRegistryAddedEvent();
}
