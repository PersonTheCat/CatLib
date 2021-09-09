package personthecat.catlib.mixin;

import com.mojang.serialization.DynamicOps;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess.RegistryHolder;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.RegistryReadOps.ResourceAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.event.registry.RegistryAccessEvent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Mixin(RegistryReadOps.class)
public class RegistryReadOpsMixin {

    @Inject(method = "create(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/resources/RegistryReadOps$ResourceAccess;Lnet/minecraft/core/RegistryAccess$RegistryHolder;)Lnet/minecraft/resources/RegistryReadOps;", at = @At("RETURN"))
    private static <T> void afterCreation(DynamicOps<T> ops, ResourceAccess access, RegistryHolder holder, CallbackInfoReturnable<RegistryReadOps<T>> ci) {
        if (!Thread.currentThread().getName().contains("Netty")) RegistryAccessEvent.EVENT.invoker().accept(holder);
    }
}
