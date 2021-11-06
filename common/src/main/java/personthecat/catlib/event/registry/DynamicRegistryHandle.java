package personthecat.catlib.event.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.util.RegistryUtils;

import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class DynamicRegistryHandle<T> implements RegistryHandle<T> {

    private volatile RegistryHandle<T> wrapped;

    private DynamicRegistryHandle(final RegistryHandle<T> wrapped) {
        this.wrapped = wrapped;
    }

    public static <T> DynamicRegistryHandle<T> createHandle(final ResourceKey<Registry<T>> key) {
        return new DynamicRegistryHandle<>(RegistryUtils.tryGetHandle(key).orElse(DummyRegistryHandle.getInstance()));
    }

    public synchronized void updateRegistry(final RegistryHandle<T> updated) {
        this.wrapped = updated;
    }

    @Nullable
    @Override
    public ResourceLocation getKey(final T t) {
        return this.wrapped.getKey(t);
    }

    @Nullable
    @Override
    public T lookup(final ResourceLocation id) {
        return this.wrapped.lookup(id);
    }

    @Override
    public <V extends T> V register(final ResourceLocation id, final V v) {
        return this.wrapped.register(id, v);
    }

    @Override
    public void forEach(final BiConsumer<ResourceLocation, T> f) {
        this.wrapped.forEach(f);
    }

    @Override
    public boolean isRegistered(final ResourceLocation id) {
        return this.wrapped.isRegistered(id);
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return this.wrapped.keySet();
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this.wrapped.iterator();
    }

    @Override
    public Stream<T> stream() {
        return this.wrapped.stream();
    }
}
