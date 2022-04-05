package personthecat.catlib.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public interface RegistryHandle<T> extends Iterable<T> {
    @Nullable ResourceLocation getKey(final T t);
    @Nullable T lookup(final ResourceLocation id);
    <V extends T> V register(final ResourceLocation id, V v);
    void forEach(final BiConsumer<ResourceLocation, T> f);
    boolean isRegistered(final ResourceLocation id);
    Set<ResourceLocation> keySet();
    Set<Map.Entry<ResourceKey<T>, T>> entrySet();
    Stream<T> stream();

    default Stream<ResourceLocation> streamKeys() {
        return this.keySet().stream();
    }

    default Stream<Map.Entry<ResourceKey<T>, T>> streamEntries() {
        return this.entrySet().stream();
    }

    default int getId() {
        return System.identityHashCode(this);
    }
}
