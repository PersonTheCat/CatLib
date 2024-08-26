package personthecat.catlib.registry.fabric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.registry.DynamicRegistryHandle;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.registry.RegistryHandle;

import static net.fabricmc.fabric.api.event.registry.DynamicRegistries.register;

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

    public static <T> RegistryHandle<T> createDynamic(
            final ModDescriptor mod, final ResourceKey<Registry<T>> key, final Codec<T> elementCodec) {
        register(key, elementCodec);
        return DynamicRegistries.getOrCreate(key);
    }
}
