package personthecat.catlib.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import personthecat.catlib.event.world.RegistryAccessTracker;

import java.util.HashSet;
import java.util.Set;

@Mixin(RegistryAccess.class)
public class RegistryAccessMixin implements RegistryAccessTracker {

    private final Set<ResourceLocation> catLibModifiedBiomes = new HashSet<>();

    @Override
    public Set<ResourceLocation> getModifiedBiomes() {
        return this.catLibModifiedBiomes;
    }
}
