package personthecat.catlib.event.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.LibEvent;
import personthecat.catlib.exception.MissingElementException;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DataRegistryEvent {
    public static final LibEvent<Consumer<Source>> PRE =
        LibEvent.create(callbacks -> src -> callbacks.forEach(c -> c.accept(src)));
    public static final LibEvent<Consumer<Source>> POST =
        LibEvent.create(callbacks -> src -> callbacks.forEach(c -> c.accept(src)));

    public static interface Source {
        @Nullable
        <T> Registry<T> getRegistry(final ResourceKey<? extends Registry<T>> key);

        Stream<Registry<?>> streamRegistries();

        default <T> Registry<T> registryOrThrow(final ResourceKey<? extends Registry<T>> key) {
            final Registry<T> registry = this.getRegistry(key);
            if (registry == null) {
                throw new MissingElementException("No such registry in current layer: " + key);
            }
            return registry;
        }

        default RegistryAccess.Frozen asRegistryAccess() {
            return new RegistryAccess.Frozen() {
                @Override
                @SuppressWarnings("unchecked")
                public @NotNull <E> Optional<Registry<E>> registry(final ResourceKey<? extends Registry<? extends E>> key) {
                    return Optional.ofNullable(Source.this.getRegistry((ResourceKey<Registry<E>>) key));
                }

                @Override
                public @NotNull Stream<RegistryEntry<?>> registries() {
                    return Source.this.streamRegistries().map(this::createEntry);
                }

                private <T> RegistryEntry<T> createEntry(final Registry<T> registry) {
                    return new RegistryEntry<>(registry.key(), registry);
                }

                @Override
                public @NotNull Frozen freeze() {
                    return this;
                }
            };
        }
    }
}
