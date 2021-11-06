package personthecat.catlib.event.registry;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface RegistryHandle<T> extends Iterable<T> {
    @Nullable ResourceLocation getKey(final T t);
    @Nullable T lookup(final ResourceLocation id);
    <V extends T> V register(final ResourceLocation id, V v);
    void forEach(final BiConsumer<ResourceLocation, T> f);
    boolean isRegistered(final ResourceLocation id);
    Stream<T> stream();
}
