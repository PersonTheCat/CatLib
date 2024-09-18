package personthecat.catlib.event.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record RegistryMapSource(Map<ResourceKey<? extends Registry<?>>, Registry<?>> map) implements RegistrySource {
    public RegistryMapSource(final Stream<Registry<?>> registries) {
        this(registries.collect(Collectors.toMap(Registry::key, Function.identity())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> Registry<T> getRegistry(final ResourceKey<? extends Registry<T>> key) {
        return (Registry<T>) this.map.get(key);
    }

    @Override
    public Stream<Registry<?>> streamRegistries() {
        return this.map.values().stream();
    }
}
