package personthecat.catlib.util;

import lombok.experimental.UtilityClass;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import personthecat.catlib.event.registry.MojangRegistryHandle;
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
        final Registry<T> registry = (Registry<T>) Registry.REGISTRY.get(key.location());
        if (registry != null) {
            return Optional.of(new MojangRegistryHandle<>(registry));
        }
        return Optional.ofNullable((Registry<T>) BuiltinRegistries.REGISTRY.get(key.location()))
            .map(MojangRegistryHandle::new);
    }
}
