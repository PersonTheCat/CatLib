package personthecat.catlib.mixin;

import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import personthecat.catlib.util.DimInjector;

@Mixin(ProtoChunk.class)
public class ProtoChunkMixin implements DimInjector {

    @Unique
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
