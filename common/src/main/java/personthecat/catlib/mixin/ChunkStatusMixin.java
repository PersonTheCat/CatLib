package personthecat.catlib.mixin;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ToFullChunk;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.util.DimInjector;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ChunkStatus.class)
public class ChunkStatusMixin {

    @Inject(method = "generate", at = @At("HEAD"))
    public void injectDims(
            WorldGenContext ctx, Executor e, ToFullChunk toFullChunk,
            List<ChunkAccess> chunks, CallbackInfoReturnable<?> cir) {
        chunks.forEach(chunk -> DimInjector.setType(chunk, ctx.level().dimensionType()));
    }
}
