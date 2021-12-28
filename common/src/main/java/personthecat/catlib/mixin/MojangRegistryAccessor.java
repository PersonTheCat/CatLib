package personthecat.catlib.mixin;

import com.google.common.collect.BiMap;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MappedRegistry.class)
public interface MojangRegistryAccessor<T> {

    @Accessor
    BiMap<ResourceLocation, T> getStorage();
}
