package personthecat.catlib.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.config.LibConfig;
import personthecat.catlib.event.world.DimensionBakeEvent;

@Mixin(WorldDimensions.class)
public class WorldDimensionsMixin {

    @ModifyArg(
        method = "bake",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/MappedRegistry;<init>(Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Lifecycle;)V"),
        index = 1)
    private Lifecycle overrideLifecycle(Lifecycle lifecycle) {
        return LibConfig.suppressDimensionLifecycleWarnings() ? Lifecycle.stable() : lifecycle;
    }

    @Inject(
        method = "bake",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/WritableRegistry;freeze()Lnet/minecraft/core/Registry;", ordinal = 0))
    private void onDimensionBake(
            Registry<LevelStem> registry,
            CallbackInfoReturnable<WorldDimensions.Complete> cir,
            @Local WritableRegistry<LevelStem> writableRegistry) {
        DimensionBakeEvent.EVENT.invoker().accept(writableRegistry);
    }
}
