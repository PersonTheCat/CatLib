package personthecat.catlib.event.registry;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@Log4j2
public class MojangRegistryHandle<T> implements RegistryHandle<T> {

    private final Registry<T> registry;

    public MojangRegistryHandle(final Registry<T> registry) {
        this.registry = registry;
    }

    @Nullable
    @Override
    public T lookup(final ResourceLocation id) {
        return this.registry.get(id);
    }

    @Override
    public void register(final ResourceLocation id, final T t) {
        Registry.register(this.registry, id, t);
    }

    public Registry<T> getRegistry() {
        return this.registry;
    }
}
