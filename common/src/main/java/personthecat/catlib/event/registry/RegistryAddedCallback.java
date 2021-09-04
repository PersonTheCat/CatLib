package personthecat.catlib.event.registry;

import net.minecraft.resources.ResourceLocation;

public interface RegistryAddedCallback<T> {
    void onRegistryAdded(RegistryHandle<T> handle, ResourceLocation id, T t);
}
