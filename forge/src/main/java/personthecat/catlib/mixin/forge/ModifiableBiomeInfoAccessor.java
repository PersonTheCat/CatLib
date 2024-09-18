package personthecat.catlib.mixin.forge;

import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModifiableBiomeInfo.class)
public interface ModifiableBiomeInfoAccessor {

    @Accessor
    void setModifiedBiomeInfo(BiomeInfo info);
}
