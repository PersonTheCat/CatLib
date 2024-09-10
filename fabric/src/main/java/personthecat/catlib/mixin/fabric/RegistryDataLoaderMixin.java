package personthecat.catlib.mixin.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryDataLoader.Loader;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.event.registry.DataRegistryEvent;
import personthecat.catlib.event.registry.RegistryMapSource;

import java.util.List;

@Mixin(value = RegistryDataLoader.class)
public class RegistryDataLoaderMixin {
    @Unique
    private static final ThreadLocal<Boolean> IS_SERVER = ThreadLocal.withInitial(() -> false);

    @WrapOperation(
        method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;"))
    private static Frozen wrapIsServer(
            @Coerce Object f, RegistryAccess registries, List<RegistryData<?>> list, Operation<Frozen> original) {
        try {
            IS_SERVER.set(true);
            return original.call(f, registries, list);
        } finally {
            IS_SERVER.set(false);
        }
    }

    // beforeLoad is provided by Fabric
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
