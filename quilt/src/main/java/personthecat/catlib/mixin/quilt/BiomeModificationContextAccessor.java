package personthecat.catlib.mixin.quilt;

import net.minecraft.core.RegistryAccess;
import org.quiltmc.qsl.worldgen.biome.impl.modification.BiomeModificationContextImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//@Mixin(BiomeModificationContextImpl.class)
public interface BiomeModificationContextAccessor {

//    @Accessor(value = "registries", remap = false)
    RegistryAccess getRegistries();
}
