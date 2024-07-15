package personthecat.catlib.registry.fabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class MojangRegistryHandleImpl {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T, V extends T> void doDeferredRegister(
            ResourceKey<? extends Registry<T>> key, String modId, ResourceLocation id, V v) {
        final Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get((ResourceKey) key);
        if (registry == null) throw new IllegalArgumentException("Unknown registry key: " + key);
        Registry.register(registry, modId, v);
    }
}
