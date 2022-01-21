package personthecat.catlib.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.catlib.event.registry.DynamicRegistries;
import personthecat.catlib.util.DimInjector;

import java.util.function.Supplier;

@Mixin(LevelStem.class)
public class LevelStemMixin {

    private static final Logger CATLIB_LOG = LogManager.getLogger("LevelStemMixin");

    @Inject(method = "<init>", at = @At("TAIL"))
    public void injectDim(final Supplier<DimensionType> dim, final ChunkGenerator chunk, final CallbackInfo ci) {
        try {
            final DimInjector injector = (DimInjector) chunk;
            final DimensionType get = dim.get();
            if (get != null) {
                injector.setType(get);
            } else if (injector.getType() != null) {
                final ResourceLocation original = DynamicRegistries.DIMENSION_TYPES.getKey(injector.getType());
                CATLIB_LOG.error("Level stem created with invalid dim key. Reusing {} in chunk {}", original, chunk);
            } else {
                CATLIB_LOG.error("Level stem created with invalid dim key. The dimension of {} will be invisible", chunk);
            }
        } catch (final RuntimeException e) {
            CATLIB_LOG.error("Error injecting dim key into " + chunk + ". Its dimension will be invisible.", e);
        }
    }
}
