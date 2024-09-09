package personthecat.catlib.mixin.fabric;

import net.fabricmc.fabric.impl.biome.modification.BiomeModificationContextImpl;
import net.minecraft.core.RegistryAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("UnstableApiUsage")
@Mixin(BiomeModificationContextImpl.class)
public interface BiomeModificationContextAccessor {

    @Accessor(value = "registries", remap = false)
    RegistryAccess getRegistries();
}
