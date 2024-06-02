package personthecat.catlib.registry.forge;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.registry.DynamicRegistryHandle;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.registry.RegistryHandle;

public final class RegistryHandleImpl {
    private RegistryHandleImpl() {}

    public static <T> RegistryHandle<T> create(final ResourceKey<Registry<T>> key) {
        final RegistryBuilder<T> builder = RegistryBuilder.<T>of(key.location()).allowModification().hasTags();
        final DeferredRegister<T> dr = DeferredRegister.create(key, key.location().getNamespace());
        return new ForgeRegistryHandle<>(dr.makeRegistry(() -> builder).get());
    }

    @SuppressWarnings("UnstableApiUsage")
    public static <T> void addToRoot(final ModDescriptor mod, RegistryHandle<T> handle) {
        if (handle instanceof DynamicRegistryHandle<T> dynamic) {
            handle = dynamic.getWrapped();
        }
        if (handle instanceof MojangRegistryHandle) {
            throw new IllegalArgumentException("Forge platform requires ForgeRegistry impl. Cannot register Mojang registry directly");
        }
        if (!(handle instanceof ForgeRegistryHandle<T> forge)) {
            throw new IllegalArgumentException("Unsupported registry handle: " + handle.getClass().getSimpleName());
        }
        final ModContainer container = ModLoadingContext.get().getActiveContainer();
        final String expected = mod.getModId();
        final String modId = container.getModId();
        if (!expected.equals(modId)) {
            throw new IllegalStateException("Attempted to register registry for " + expected + " from mod" + modId);
        }
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((NewRegistryEvent e) -> e.create(forge.getRegistry().getBuilder()));
    }
}
