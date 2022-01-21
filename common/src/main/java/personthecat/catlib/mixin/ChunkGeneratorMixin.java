package personthecat.catlib.mixin;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import personthecat.catlib.util.DimInjector;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin implements DimInjector {
    DimensionType catlibInjectedDim;

    @Override
    public void setType(final @NotNull DimensionType type) {
        this.catlibInjectedDim = type;
    }

    @Override
    public @Nullable DimensionType getType() {
        return this.catlibInjectedDim;
    }
}
