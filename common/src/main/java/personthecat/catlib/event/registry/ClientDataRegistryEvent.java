package personthecat.catlib.event.registry;

import personthecat.catlib.event.LibEvent;

import java.util.function.Consumer;

public class ClientDataRegistryEvent {
    public static final LibEvent<Consumer<RegistrySource>> PRE =
        LibEvent.create(callbacks -> src -> callbacks.forEach(c -> c.accept(src)));
    public static final LibEvent<Consumer<RegistrySource>> POST =
        LibEvent.create(callbacks -> src -> callbacks.forEach(c -> c.accept(src)));
}
