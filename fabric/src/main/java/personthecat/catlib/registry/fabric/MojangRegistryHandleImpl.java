package personthecat.catlib.registry.fabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

public class MojangRegistryHandleImpl {

    @SuppressWarnings("unchecked")
    public static <T, V extends T> void doDeferredRegister(String modId, ResourceKey<T> key, V v) {
        final Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(key.registry());
        if (registry == null) throw new IllegalArgumentException("Unknown registry key: " + key);
        Registry.register(registry, key, v);
    }
}
