package personthecat.catlib.mixin;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.ForgeRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ForgeRegistry.class)
public interface ForgeRegistryAccessor<T> {

    @Accessor(remap = false)
    ResourceKey<Registry<T>> getKey();
}
