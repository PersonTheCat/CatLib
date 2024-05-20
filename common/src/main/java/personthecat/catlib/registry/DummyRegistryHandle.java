package personthecat.catlib.registry;

import it.unimi.dsi.fastutil.objects.ObjectIterators;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@Log4j2
public class DummyRegistryHandle<T> implements RegistryHandle<T> {

    private static final RegistryHandle<?> INSTANCE = new DummyRegistryHandle<>();

    private DummyRegistryHandle() {}

    @SuppressWarnings("unchecked")
    public static <T> RegistryHandle<T> getInstance() {
        return (RegistryHandle<T>) INSTANCE;
    }

    @Override
    public @Nullable ResourceLocation getKey(final T t) {
        return null;
    }

    @Override
    public @Nullable T lookup(final ResourceLocation id) {
        return null;
    }

    @Override
    public <V extends T> V register(final ResourceLocation id, V v) {
        log.error("Attempted to register through dummy handle. This will have no effect.");
        return v;
    }

    @Override
    public void forEach(final BiConsumer<ResourceLocation, T> f) {}

    @Override
    public void forEachHolder(final BiConsumer<ResourceLocation, Holder<T>> f) {}

    @Override
    public boolean isRegistered(final ResourceLocation id) {
        return false;
    }

    @Override
    public @Nullable Holder<T> getHolder(final ResourceLocation id) {
        return null;
    }

    @Override
    public Map<TagKey<T>, HolderSet.Named<T>> getTags() {
        return Collections.emptyMap();
    }

    @Override
    public @Nullable ResourceKey<? extends Registry<T>> key() {
        return null;
    }

    @Override
    public Collection<Holder<T>> holders() {
        return Collections.emptySet();
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return Collections.emptySet();
    }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        return Collections.emptySet();
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return ObjectIterators.emptyIterator();
    }

    @Override
    public HolderLookup<T> asLookup() {
        return new HolderLookup<T>() {
            @Override
            public Stream<Holder.Reference<T>> listElements() {
                return Stream.empty();
            }

            @Override
            public Stream<HolderSet.Named<T>> listTags() {
                return Stream.empty();
            }

            @Override
            public Optional<Holder.Reference<T>> get(final @NotNull ResourceKey<T> resourceKey) {
                return Optional.empty();
            }

            @Override
            public Optional<HolderSet.Named<T>> get(final @NotNull TagKey<T> tagKey) {
                return Optional.empty();
            }
        };
    }

    @Override
    public Stream<T> stream() {
        return Stream.empty();
    }

    @Override
    public int size() {
        return 0;
    }
}
