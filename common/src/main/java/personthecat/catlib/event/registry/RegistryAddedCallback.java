package personthecat.catlib.event.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import personthecat.catlib.registry.RegistryHandle;

public interface RegistryAddedCallback<T> {
    void onRegistryAdded(RegistryHandle<T> handle, ResourceKey<T> key, T t);
}
