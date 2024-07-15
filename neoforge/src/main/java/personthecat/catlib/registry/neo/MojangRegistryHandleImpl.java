package personthecat.catlib.registry.neo;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.RegisterEvent;
import personthecat.catlib.exception.MissingElementException;

import java.util.Optional;

public class MojangRegistryHandleImpl {
    public static <T, V extends T> void doDeferredRegister(
            ResourceKey<? extends Registry<T>> key, String modId, ResourceLocation id, V v) {
        ModList.get().getModContainerById(modId)
            .flatMap(container -> Optional.ofNullable(container.getEventBus()))
            .orElseThrow(() -> new MissingElementException("No event bus for mod: " + modId))
            .addListener((RegisterEvent e) -> e.register(key, id, () -> v));
    }
}
