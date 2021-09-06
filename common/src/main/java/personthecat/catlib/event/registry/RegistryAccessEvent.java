package personthecat.catlib.event.registry;

import net.minecraft.core.RegistryAccess;
import personthecat.catlib.event.LibEvent;

import java.util.function.Consumer;

public class RegistryAccessEvent {
    public static final LibEvent<Consumer<RegistryAccess>> EVENT =
        LibEvent.create(callbacks -> access -> callbacks.forEach(c -> c.accept(access)));
}
