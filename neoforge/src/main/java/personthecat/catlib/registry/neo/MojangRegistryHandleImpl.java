package personthecat.catlib.registry.neo;

import net.minecraft.resources.ResourceKey;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.RegisterEvent;
import personthecat.catlib.exception.MissingElementException;

import java.util.Optional;

public class MojangRegistryHandleImpl {
    public static <T, V extends T> void doDeferredRegister(String modId, ResourceKey<T> key, V v) {
        ModList.get().getModContainerById(modId)
            .flatMap(container -> Optional.ofNullable(container.getEventBus()))
            .orElseThrow(() -> new MissingElementException("No event bus for mod: " + modId))
            .addListener((RegisterEvent e) -> e.register(key.registryKey(), key.location(), () -> v));
    }
}
