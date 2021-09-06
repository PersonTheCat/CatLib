package personthecat.catlib.event.registry;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface RegistryHandle<T> extends Iterable<T> {
    @Nullable ResourceLocation getKey(final T t);
    @Nullable T lookup(final ResourceLocation id);
    void register(final ResourceLocation id, T t);
    void forEach(final BiConsumer<ResourceLocation, T> f);

    default boolean isRegistered(final ResourceLocation id) {
        return this.lookup(id) != null;
    }

    default void forEachValue(final Consumer<T> f) {
        this.forEach((id, t) -> f.accept(t));
    }
}
