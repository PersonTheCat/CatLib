package personthecat.catlib.event.registry;

import personthecat.catlib.event.LibEvent;

public interface RegistryEventAccessor<T> {
    LibEvent<RegistryAddedCallback<T>> getRegistryAddedEvent();
}
