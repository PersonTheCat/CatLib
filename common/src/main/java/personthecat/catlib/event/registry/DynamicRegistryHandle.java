package personthecat.catlib.event.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.util.RegistryUtils;

import java.util.Iterator;
import java.util.function.BiConsumer;

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
    public void register(final ResourceLocation id, final T t) {
        this.wrapped.register(id, t);
    }

    @Override
    public void forEach(final BiConsumer<ResourceLocation, T> f) {
        this.wrapped.forEach(f);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this.wrapped.iterator();
    }
}
