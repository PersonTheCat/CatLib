package personthecat.catlib.registry.forge;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.registries.RegisterEvent;
import personthecat.catlib.exception.MissingElementException;

import java.util.Optional;

public class MojangRegistryHandleImpl {
    public static <T, V extends T> void doDeferredRegister(
            ResourceKey<? extends Registry<T>> key, String modId, ResourceLocation id, V v) {
        ModList.get().getModContainerById(modId)
            .flatMap(c -> Optional.ofNullable(c instanceof FMLModContainer mod ? mod.getEventBus() : null))
            .orElseThrow(() -> new MissingElementException("No event bus for mod: " + modId))
            .addListener((RegisterEvent e) -> e.register(key, id, () -> v));
    }
}
