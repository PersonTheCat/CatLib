package personthecat.catlib.event.registry;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface RegistryHandle<T> {
    @Nullable T lookup(final ResourceLocation id);
    void register(final ResourceLocation id, T t);

    default boolean isRegistered(final ResourceLocation id) {
        return this.lookup(id) != null;
    }
}
