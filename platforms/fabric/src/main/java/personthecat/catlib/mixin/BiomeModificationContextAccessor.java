package personthecat.catlib.mixin;

import net.fabricmc.fabric.impl.biome.modification.BiomeModificationContextImpl;
import net.minecraft.core.RegistryAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeModificationContextImpl.class)
public interface BiomeModificationContextAccessor {

    @Accessor(value = "registries", remap = false)
    RegistryAccess getRegistries();
}
