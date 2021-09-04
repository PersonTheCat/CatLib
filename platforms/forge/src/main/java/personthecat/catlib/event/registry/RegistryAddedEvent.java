package personthecat.catlib.event.registry;

import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import personthecat.catlib.event.LibEvent;
import personthecat.catlib.exception.RegistryLookupException;
import personthecat.overwritevalidator.annotations.OverwriteClass;

@Log4j2
@OverwriteClass
public class RegistryAddedEvent {

    @SuppressWarnings("unchecked")
    public static <T> LibEvent<RegistryAddedCallback<T>> get(final ResourceKey<Registry<T>> key) {
        final IForgeRegistry<?> forgeRegistry = RegistryManager.ACTIVE.getRegistry(key.location());
        if (forgeRegistry != null) {
            return ((RegistryEventAccessor<T>) forgeRegistry).getRegistryAddedEvent();
        }
        final Registry<T> mojangRegistry = (Registry<T>) Registry.REGISTRY.get(key.location());
        if (mojangRegistry != null) {
            return ((RegistryEventAccessor<T>) mojangRegistry).getRegistryAddedEvent();
        }
        final Registry<T> builtinRegistry = (Registry<T>) BuiltinRegistries.REGISTRY.get(key.location());
        if (builtinRegistry != null) {
            return ((RegistryEventAccessor<T>) builtinRegistry).getRegistryAddedEvent();
        }
        throw new RegistryLookupException(key);
    }
}
