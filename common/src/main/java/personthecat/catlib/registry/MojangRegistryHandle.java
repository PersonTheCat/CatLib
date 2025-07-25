package personthecat.catlib.registry;

import com.mojang.serialization.Codec;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.mixin.MappedRegistryAccessor;

import java.util.Collection;
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
    public @Nullable ResourceKey<T> getKey(final T t) {
        return this.registry.getResourceKey(t).orElse(null);
    }

    @Override
    public @Nullable ResourceLocation getId(final T t) {
        return this.registry.getKey(t);
    }

    @Override
    public @Nullable T lookup(final ResourceKey<T> key) {
        return this.registry.get(key);
    }

    @Override
    public @Nullable T lookup(final ResourceLocation id) {
        return this.registry.get(id);
    }

    @Override
    public <V extends T> V register(final ResourceKey<T> key, final V v) {
        ((WritableRegistry<T>) this.registry).register(key, v, RegistrationInfo.BUILT_IN);
        return v;
    }

    @Override
    public <V extends T> void deferredRegister(final String modId, final ResourceLocation id, final V v) {
        doDeferredRegister(this.registry.key(), modId, id, v);
    }

    @Override
    public void forEach(final BiConsumer<ResourceKey<T>, T> f) {
        for (final Map.Entry<ResourceKey<T>, T> entry : new HashSet<>(this.registry.entrySet())) {
            f.accept(entry.getKey(), entry.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEachHolder(final BiConsumer<ResourceKey<T>, Holder<T>> f) {
        if (this.registry instanceof MappedRegistry<T> mapped) {
            ((MappedRegistryAccessor<T>) mapped).getHolderMap().forEach(f);
            return;
        }
        throw new UnsupportedOperationException("Unsupported registry in handle: " + this.registry.getClass());
    }

    @Override
    public boolean isRegistered(final ResourceKey<T> key) {
        return this.registry.containsKey(key);
    }

    @Override
    public boolean isRegistered(final ResourceLocation id) {
        return this.registry.containsKey(id);
    }

    @Override
    public @Nullable Holder<T> getHolder(final ResourceKey<T> k) {
        return this.registry.getHolder(k).orElse(null);
    }

    @Override
    public @Nullable Holder<T> getHolder(final ResourceLocation id) {
        return this.registry.getHolder(id).orElse(null);
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
    public @Nullable HolderSet.Named<T> getNamed(final TagKey<T> key) {
        return this.registry.getTag(key).orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResourceKey<Registry<T>> key() {
        return (ResourceKey<Registry<T>>) this.registry.key();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Holder<T>> holders() {
        if (this.registry instanceof MappedRegistry<T> mapped) {
            return ((MappedRegistryAccessor<T>) mapped).getHolderMap().values();
        }
        throw new UnsupportedOperationException("Unsupported registry in handle: " + this.registry.getClass());
    }

    @Override
    public Set<ResourceKey<T>> keySet() {
        return this.registry.registryKeySet();
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
    public HolderLookup<T> asLookup() {
        return this.registry.asLookup();
    }

    @Override
    public Registry<T> asRegistry() {
        return this.registry;
    }

    @Override
    public Stream<T> stream() {
        return this.registry.stream();
    }

    @Override
    public Codec<Holder<T>> holderCodec() {
        return this.registry.holderByNameCodec();
    }

    @Override
    public Codec<T> codec() {
        return this.registry.byNameCodec();
    }

    @Override
    public int size() {
        return this.registry.size();
    }

    @ExpectPlatform
    private static <T, V extends T> void doDeferredRegister(
            final ResourceKey<? extends Registry<T>> key, final String modId, final ResourceLocation id, final V t) {}
}
