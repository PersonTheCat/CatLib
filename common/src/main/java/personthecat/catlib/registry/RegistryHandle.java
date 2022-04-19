package personthecat.catlib.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public interface RegistryHandle<T> extends Iterable<T> {
    @Nullable ResourceLocation getKey(final T t);
    @Nullable T lookup(final ResourceLocation id);
    <V extends T> V register(final ResourceLocation id, V v);
    void forEach(final BiConsumer<ResourceLocation, T> f);
    boolean isRegistered(final ResourceLocation id);
    @Nullable Holder<T> getHolder(final ResourceLocation id);
    @Nullable Collection<T> getTag(final TagKey<T> key);
    @Nullable ResourceKey<? extends Registry<T>> key();
    Set<ResourceLocation> keySet();
    Set<Map.Entry<ResourceKey<T>, T>> entrySet();
    Stream<T> stream();

    default @Nullable Collection<T> getTag(final ResourceLocation id) {
        final ResourceKey<? extends Registry<T>> key = this.key();
        return key != null ? this.getTag(TagKey.create(key, id)) : null;
    }

    default ResourceKey<? extends Registry<T>> keyOrThrow() {
        return Objects.requireNonNull(this.key(), "Polled key from dummy container");
    }

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
