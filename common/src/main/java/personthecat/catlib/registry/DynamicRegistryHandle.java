package personthecat.catlib.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.registry.DataRegistryEvent;
import personthecat.catlib.serialization.codec.CodecUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DynamicRegistryHandle<T> implements RegistryHandle<T> {

    private final Map<Object, BiConsumer<Object, RegistryHandle<T>>> synchronizer = new WeakHashMap<>();
    private volatile RegistryHandle<T> wrapped;

    private DynamicRegistryHandle(final RegistryHandle<T> wrapped) {
        this.wrapped = wrapped;
    }

    public static <T> DynamicRegistryHandle<T> createHandle(final ResourceKey<? extends Registry<T>> key) {
        return new DynamicRegistryHandle<>(RegistryUtils.tryGetHandle(key).orElseGet(() -> new DummyRegistryHandle<>(key)));
    }

    public synchronized void updateRegistry(final RegistryHandle<T> updated) {
        this.wrapped = updated;
        this.synchronizer.forEach((mutex, listener) -> listener.accept(mutex, updated));
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public synchronized void listen(final Object mutex, final Consumer<RegistryHandle<T>> listener) {
        this.synchronizer.put(mutex, (m, handle) -> {
            // Prevent this lambda from strongly referencing the mutex.
            synchronized (m) {
                listener.accept(handle);
            }
        });
    }

    public RegistryHandle<T> getWrapped() {
        return this.wrapped;
    }

    @Nullable
    @Override
    public ResourceKey<T> getKey(final T t) {
        return this.wrapped.getKey(t);
    }

    @Override
    public ResourceLocation getId(final T t) {
        return this.wrapped.getId(t);
    }

    @Nullable
    @Override
    public T lookup(final ResourceKey<T> key) {
        return this.wrapped.lookup(key);
    }

    @Nullable
    @Override
    public T lookup(final ResourceLocation id) {
        return this.wrapped.lookup(id);
    }

    @Override
    public <V extends T> V register(final ResourceKey<T> key, final V v) {
        return this.wrapped.register(key, v);
    }

    @Override
    public <V extends T> V register(final ResourceLocation id, final V v) {
        return this.wrapped.register(id, v);
    }

    @Override
    public <V extends T> void deferredRegister(final String modId, final ResourceLocation id, final V v) {
        if (!(this.wrapped instanceof DummyRegistryHandle)) {
            this.wrapped.deferredRegister(modId, id, v);
            return;
        }
        DataRegistryEvent.PRE.register(src -> {
            final Registry<T> registry = src.getRegistry(this.key());
            if (registry != null) {
                Registry.register(registry, id, v);
            }
        });
    }

    @Override
    public void forEach(final BiConsumer<ResourceKey<T>, T> f) {
        this.wrapped.forEach(f);
    }

    @Override
    public void forEachHolder(final BiConsumer<ResourceKey<T>, Holder<T>> f) {
        this.wrapped.forEachHolder(f);
    }

    @Override
    public boolean isRegistered(final ResourceKey<T> key) {
        return this.wrapped.isRegistered(key);
    }

    @Override
    public boolean isRegistered(final ResourceLocation id) {
        return this.wrapped.isRegistered(id);
    }

    @Override
    public @Nullable Holder<T> getHolder(ResourceKey<T> key) {
        return this.wrapped.getHolder(key);
    }

    @Override
    public @Nullable Holder<T> getHolder(final ResourceLocation id) {
        return this.wrapped.getHolder(id);
    }

    @Override
    public Map<TagKey<T>, HolderSet.Named<T>> getTags() {
        return this.wrapped.getTags();
    }

    @Override
    public HolderSet.@Nullable Named<T> getNamed(final TagKey<T> key) {
        return this.wrapped.getNamed(key);
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.wrapped.key();
    }

    @Override
    public Collection<? extends Holder<T>> holders() {
        return this.wrapped.holders();
    }

    @Override
    public Set<ResourceKey<T>> keySet() {
        return this.wrapped.keySet();
    }

    @Override
    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        return this.wrapped.entrySet();
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this.wrapped.iterator();
    }

    @Override
    public HolderLookup<T> asLookup() {
        return this.wrapped.asLookup();
    }

    @Override
    public Stream<T> stream() {
        return this.wrapped.stream();
    }

    @Override
    public Codec<Holder<T>> holderCodec() {
        return RegistryFileCodec.create(this.key(), CodecUtils.neverCodec(), false);
    }

    @Override
    public Codec<T> codec() {
        return this.holderCodec().flatComapMap(Holder::value, this::tryGetHolder);
    }

    @Override
    public int size() {
        return this.wrapped.size();
    }

    @Override
    public Registry<T> asRegistry() {
        return this.wrapped.asRegistry();
    }

    @Override
    public int getId() {
        return this.wrapped.getId();
    }
}
