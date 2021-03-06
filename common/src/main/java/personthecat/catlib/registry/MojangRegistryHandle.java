package personthecat.catlib.registry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.mixin.MappedRegistryAccessor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class MojangRegistryHandle<T> implements RegistryHandle<T> {

    private final Registry<T> registry;

    public MojangRegistryHandle(final Registry<T> registry) {
        this.registry = registry;
    }

    public Registry<T> getRegistry() {
        return this.registry;
    }

    @Override
    public @Nullable ResourceLocation getKey(final T t) {
        return this.registry.getKey(t);
    }

    @Override
    public @Nullable T lookup(final ResourceLocation id) {
        return this.registry.get(id);
    }

    @Override
    public <V extends T> V register(final ResourceLocation id, final V v) {
        ((WritableRegistry<T>) this.registry).register(ResourceKey.create(this.registry.key(), id), v, Lifecycle.stable());
        return v;
    }

    @Override
    public void forEach(final BiConsumer<ResourceLocation, T> f) {
        for (final Map.Entry<ResourceKey<T>, T> entry : new HashSet<>(this.registry.entrySet())) {
            f.accept(entry.getKey().location(), entry.getValue());
        }
    }

    @Override
    public boolean isRegistered(final ResourceLocation id) {
        return this.registry.containsKey(id);
    }

    @Override
    public @Nullable Holder<T> getHolder(final ResourceLocation id) {
        return this.registry.getHolder(ResourceKey.create(this.registry.key(), id)).orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<TagKey<T>, HolderSet.Named<T>> getTags() {
        if (this.registry instanceof MappedRegistry<T> mapped) {
            return ((MappedRegistryAccessor<T>) mapped).getTagsDirectly();
        }
        throw new UnsupportedOperationException("Unsupported registry in handle: " + this.registry.getClass());
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.registry.key();
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return this.registry.keySet();
    }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        return this.registry.entrySet();
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return this.registry.iterator();
    }

    @Override
    public Stream<T> stream() {
        return this.registry.stream();
    }
}
