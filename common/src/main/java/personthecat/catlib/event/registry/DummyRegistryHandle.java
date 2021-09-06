package personthecat.catlib.event.registry;

import it.unimi.dsi.fastutil.objects.ObjectIterators;
import lombok.extern.log4j.Log4j2;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.BiConsumer;

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
    public void register(final ResourceLocation id, T t) {
        log.error("Attempt to register into dummy handle. This will have no effect.");
    }

    @Override
    public void forEach(final BiConsumer<ResourceLocation, T> f) {}

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return ObjectIterators.emptyIterator();
    }
}
