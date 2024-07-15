package personthecat.catlib.event.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.LibEvent;
import personthecat.catlib.exception.MissingElementException;

import java.util.function.Consumer;

public class DataRegistryEvent {
    public static final LibEvent<Consumer<Source>> PRE =
        LibEvent.create(callbacks -> src -> callbacks.forEach(c -> c.accept(src)));
    public static final LibEvent<Consumer<Source>> POST =
        LibEvent.create(callbacks -> src -> callbacks.forEach(c -> c.accept(src)));

    public interface Source {
        @Nullable <T> Registry<T> getRegistry(final ResourceKey<? extends Registry<T>> key);

        default <T> Registry<T> registryOrThrow(final ResourceKey<? extends Registry<T>> key) {
            final Registry<T> registry = this.getRegistry(key);
            if (registry == null) {
                throw new MissingElementException("No such registry in current layer: " + key);
            }
            return registry;
        }
    }
}
