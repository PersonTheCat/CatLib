package personthecat.catlib.registry.neo;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.neoforged.neoforge.registries.RegistryManager;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.registry.DynamicRegistryHandle;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.registry.RegistryHandle;

import java.util.Objects;

public final class RegistryHandleImpl {
    private RegistryHandleImpl() {}

    public static <T> RegistryHandle<T> create(final ResourceKey<Registry<T>> key) {
        return new MojangRegistryHandle<>(new RegistryBuilder<>(key).disableRegistrationCheck().create());
    }

    @SuppressWarnings("UnstableApiUsage")
    public static <T> void addToRoot(final ModDescriptor mod, RegistryHandle<T> handle) {
        if (handle instanceof DynamicRegistryHandle<T> dynamic) {
            handle = dynamic.getWrapped();
        }
        if (!(handle instanceof MojangRegistryHandle<T> mojang)) {
            throw new IllegalArgumentException("Unsupported handle, cannot add to root: " + handle.getClass().getSimpleName());
        }
        final ResourceKey<? extends Registry<T>> key = Objects.requireNonNull(handle.key(), "No key in registry");
        final IEventBus modBus = getEventBusForMod(mod);
        RegistryManager.trackModdedRegistry(key.location());
        modBus.addListener((NewRegistryEvent e) -> e.register(mojang.getRegistry()));
    }

    public static <T> RegistryHandle<T> createDynamic(
            final ModDescriptor mod, final ResourceKey<Registry<T>> key, final Codec<T> elementCodec) {
        final IEventBus modBus = getEventBusForMod(mod);
        modBus.addListener((DataPackRegistryEvent.NewRegistry e) -> e.dataPackRegistry(key, elementCodec));
        return DynamicRegistries.getOrCreate(key);
    }

    private static IEventBus getEventBusForMod(final ModDescriptor mod) {
        final ModContainer container = ModLoadingContext.get().getActiveContainer();
        final String expected = mod.getModId();
        final String modId = container.getModId();
        if (!expected.equals(modId)) {
            throw new IllegalStateException("Attempted to register registry for " + expected + " from mod" + modId);
        }
        return Objects.requireNonNull(container.getEventBus(), "No event bus for mod: " + modId);
    }
}
