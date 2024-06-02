package personthecat.catlib.registry;

import com.mojang.serialization.Lifecycle;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;

import java.util.Collection;
import java.util.Collections;
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
    void forEachHolder(final BiConsumer<ResourceLocation, Holder<T>> f);
    boolean isRegistered(final ResourceLocation id);
    @Nullable Holder<T> getHolder(final ResourceLocation id);
    Map<TagKey<T>, HolderSet.Named<T>> getTags();
    @Nullable ResourceKey<? extends Registry<T>> key();
    Collection<? extends Holder<T>> holders();
    Set<ResourceLocation> keySet();
    Set<Map.Entry<ResourceKey<T>, T>> entrySet();
    HolderLookup<T> asLookup();
    Stream<T> stream();
    int size();

    @ExpectPlatform
    static <T> RegistryHandle<T> create(final ResourceKey<Registry<T>> key) {
        return new MojangRegistryHandle<>(new MappedRegistry<>(key, Lifecycle.stable()));
    }

    @ExpectPlatform
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> void addToRoot(final ModDescriptor mod, final RegistryHandle<T> handle) {
        if (handle instanceof MojangRegistryHandle<T> m) { // platforms will handle other types, events, etc
            Registry.register((Registry) BuiltInRegistries.REGISTRY, (ResourceKey) m.key(), m.getRegistry());
        }
    }

    default @Nullable ResourceLocation keyOf(final Holder<T> holder) {
        return holder.unwrapKey()
            .map(ResourceKey::location) // value must be present if no location
            .orElseGet(() -> this.getKey(holder.value()));
    }

    default @Nullable HolderSet.Named<T> getNamed(final TagKey<T> key) {
        return this.getTags().get(key);
    }

    default @Nullable Holder<T> getHolder(final T t) {
        final ResourceLocation l = this.getKey(t);
        return l != null ? this.getHolder(l) : null;
    }

    default Collection<T> getTag(final TagKey<T> key) {
        final HolderSet.Named<T> named = this.getNamed(key);
        if (named == null) return Collections.emptyList();
        return named.stream().map(Holder::value).toList();
    }

    default Collection<T> getTag(final ResourceLocation id) {
        final ResourceKey<? extends Registry<T>> key = this.key();
        return key != null ? this.getTag(TagKey.create(key, id)) : Collections.emptySet();
    }

    default ResourceKey<? extends Registry<T>> keyOrThrow() {
        return Objects.requireNonNull(this.key(), "Polled key from dummy container");
    }

    default boolean isEmpty() {
        return this.size() == 0;
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
