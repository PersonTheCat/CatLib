package personthecat.catlib.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.util.DimInjector;

import java.util.List;
import java.util.function.Function;

@Mixin(value = ChunkStatus.class)
public class ChunkStatusMixin {

    @Inject(method = "generate", at = @At("HEAD"))
    public void injectDims(
            ServerLevel l, ChunkGenerator g, StructureManager m, ThreadedLevelLightEngine e, Function<?, ?> f,
            List<ChunkAccess> chunks, CallbackInfoReturnable<?> cir) {
        chunks.forEach(chunk -> DimInjector.setType(chunk, l.dimensionType()));
    }
}
