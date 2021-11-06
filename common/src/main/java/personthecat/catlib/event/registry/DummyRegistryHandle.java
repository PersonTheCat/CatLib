package personthecat.catlib.event.registry;

import it.unimi.dsi.fastutil.objects.ObjectIterators;
import lombok.extern.log4j.Log4j2;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
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

    @Nullable
    @Override
    public ResourceLocation getKey(final T t) {
        return null;
    }

    @Nullable
    @Override
    public T lookup(final ResourceLocation id) {
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
    public boolean isRegistered(final ResourceLocation id) {
        return false;
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return ObjectIterators.emptyIterator();
    }

    @Override
    public Stream<T> stream() {
        return Stream.empty();
    }
}
