package personthecat.catlib.mixin.neo;

import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModifiableBiomeInfo.class)
public interface ModifiableBiomeInfoAccessor {

    @Accessor
    void setModifiedBiomeInfo(BiomeInfo info);
}
