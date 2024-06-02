package personthecat.catlib.registry.neo;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.neoforged.neoforge.registries.RegistryManager;
import personthecat.catlib.data.ModDescriptor;
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
        final ModContainer container = ModLoadingContext.get().getActiveContainer();
        final String expected = mod.getModId();
        final String modId = container.getModId();
        if (!expected.equals(modId)) {
            throw new IllegalStateException("Attempted to register registry for " + expected + " from mod" + modId);
        }
        final ResourceKey<? extends Registry<T>> key = Objects.requireNonNull(handle.key(), "No key in registry");
        final IEventBus modBus = Objects.requireNonNull(container.getEventBus(), "No event bus for mod: " + modId);
        RegistryManager.trackModdedRegistry(key.location());
        modBus.addListener((NewRegistryEvent e) -> e.register(mojang.getRegistry()));
    }
}
