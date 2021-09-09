package personthecat.catlib.mixin;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryReadOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.event.registry.RegistryAccessEvent;

// Must fire before biome modification event
@Mixin(value = RegistryReadOps.class, priority = 10_000)
public class RegistryReadOpsMixin {

    @Inject(method = "create(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/resources/RegistryReadOps$ResourceAccess;Lnet/minecraft/core/RegistryAccess$RegistryHolder;)Lnet/minecraft/resources/RegistryReadOps;", at = @At("RETURN"))
    private static <T> void afterCreation(DynamicOps<T> ops, RegistryReadOps.ResourceAccess access, RegistryAccess.RegistryHolder holder, CallbackInfoReturnable<RegistryReadOps<T>> ci) {
        if (!Thread.currentThread().getName().contains("Netty")) RegistryAccessEvent.EVENT.invoker().accept(holder);
    }
}
