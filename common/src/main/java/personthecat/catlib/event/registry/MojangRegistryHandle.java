package personthecat.catlib.event.registry;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    @Override
    public ResourceLocation getKey(final T t) {
        return this.registry.getKey(t);
    }

    @Nullable
    @Override
    public T lookup(final ResourceLocation id) {
        return this.registry.get(id);
    }

    @Override
    public <V extends T> V register(final ResourceLocation id, final V v) {
        return ((WritableRegistry<T>) this.registry).register(ResourceKey.create(this.registry.key(), id), v, Lifecycle.stable());
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
    public Set<ResourceLocation> keySet() {
        return this.registry.keySet();
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this.registry.iterator();
    }

    @Override
    public Stream<T> stream() {
        return this.registry.stream();
    }
}
