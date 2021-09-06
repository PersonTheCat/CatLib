package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import personthecat.catlib.event.registry.RegistryHandle;
import personthecat.catlib.exception.MissingOverrideException;
import personthecat.overwritevalidator.annotations.Inherit;
import personthecat.overwritevalidator.annotations.InheritMissingMembers;
import personthecat.overwritevalidator.annotations.Overwrite;
import personthecat.overwritevalidator.annotations.OverwriteClass;

import java.util.Optional;

@UtilityClass
@OverwriteClass
@InheritMissingMembers
public class RegistryUtils {

    @Inherit
    public static <T> RegistryHandle<T> getHandle(final ResourceKey<Registry<T>> key) {
        throw new MissingOverrideException();
    }

    @Overwrite
    @SuppressWarnings("unchecked")
    public static <T> Optional<RegistryHandle<T>> tryGetHandle(final ResourceKey<Registry<T>> key) {
        final IForgeRegistry<?> forgeRegistry = RegistryManager.ACTIVE.getRegistry(key.location());
        if (forgeRegistry != null) {
            return Optional.of((RegistryHandle<T>) forgeRegistry);
        }
        final Registry<T> mojangRegistry = (Registry<T>) Registry.REGISTRY.get(key.location());
        if (mojangRegistry != null) {
            return Optional.of((RegistryHandle<T>) mojangRegistry);
        }
        final Registry<T> builtinRegistry = (Registry<T>) BuiltinRegistries.REGISTRY.get(key.location());
        if (builtinRegistry != null) {
            return Optional.of((RegistryHandle<T>) builtinRegistry);
        }
        return Optional.empty();
    }
}
