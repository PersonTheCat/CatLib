package personthecat.catlib.registry.fabric;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.registry.DynamicRegistryHandle;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.registry.RegistryHandle;

public final class RegistryHandleImpl {
    private RegistryHandleImpl() {}

    public static <T> RegistryHandle<T> create(final ResourceKey<Registry<T>> key) {
        return new MojangRegistryHandle<>(new MappedRegistry<>(key, Lifecycle.stable()));
    }

    public static <T> void addToRoot(final ModDescriptor mod, RegistryHandle<T> handle) {
        if (handle instanceof DynamicRegistryHandle<T> dynamic) {
            handle = dynamic.getWrapped();
        }
        if (!(handle instanceof MojangRegistryHandle<T> mojang)) {
            throw new IllegalArgumentException("Unsupported handle, cannot add to root: " + handle.getClass().getSimpleName());
        }
        FabricRegistryBuilder.from((WritableRegistry<T>) mojang.getRegistry()).buildAndRegister();
    }
}
