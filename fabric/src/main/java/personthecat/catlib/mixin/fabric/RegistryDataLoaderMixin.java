package personthecat.catlib.mixin.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryDataLoader.Loader;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.event.registry.DataRegistryEvent;
import personthecat.catlib.event.registry.RegistryMapSource;

import java.util.List;

// Must get applied after Fabric mixin
@Mixin(value = RegistryDataLoader.class, priority = 1500)
public class RegistryDataLoaderMixin {
    @Shadow(remap = false)
    private static final ThreadLocal<Boolean> IS_SERVER = ThreadLocal.withInitial(() -> false);

    @Inject(
        method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
            ordinal = 1))
    private static void afterLoad(
            @Coerce Object f, RegistryAccess registries, List<RegistryData<?>> list, CallbackInfoReturnable<Frozen> cir,
            @Local(ordinal = 1) List<Loader<?>> loaders) {
        if (IS_SERVER.get()) {
            DataRegistryEvent.POST.invoker().accept(
                new RegistryMapSource(loaders.stream().map(Loader::registry)));
        }
    }
}
