package personthecat.catlib.mixin;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.serialization.Decoder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryDataLoader.Loader;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.registry.DataRegistryEvent;
import personthecat.catlib.event.registry.RegistryMapSource;
import personthecat.catlib.exception.DetailedDataLoadException;
import personthecat.catlib.serialization.codec.context.ContextualOps;
import personthecat.catlib.serialization.codec.context.TrackingDecoder;

import java.util.List;

@Mixin(RegistryDataLoader.class)
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

    @Inject(
        method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
            ordinal = 0))
    private static void beforeLoad(
            @Coerce Object f, RegistryAccess registries, List<RegistryData<?>> list, CallbackInfoReturnable<RegistryAccess.Frozen> cir,
            @Local(ordinal = 1) List<Loader<?>> loaders,
            @Share("source") LocalRef<DataRegistryEvent.Source> source) {
        if (IS_SERVER.get()) {
            source.set(new RegistryMapSource(loaders.stream().map(Loader::registry)));
            DataRegistryEvent.PRE.invoker().accept(source.get());
        }
    }

    @Inject(
        method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
            ordinal = 1))
    private static void afterLoad(
            @Coerce Object f, RegistryAccess registries, List<RegistryData<?>> list, CallbackInfoReturnable<RegistryAccess.Frozen> cir,
            @Share("source") LocalRef<DataRegistryEvent.Source> source) {
        if (source.get() != null) {
            DataRegistryEvent.POST.invoker().accept(source.get());
        }
    }

    @WrapMethod(method = "loadElementFromResource")
    private static <E> void catchRegistryLoadError(
            WritableRegistry<E> registry,
            Decoder<E> decoder,
            RegistryOps<JsonElement> ops,
            ResourceKey<E> key,
            Resource resource,
            RegistrationInfo registration,
            Operation<Void> loadElementFromResource) {
        try {
            decoder = new TrackingDecoder<>(decoder);
            loadElementFromResource.call(registry, decoder, ops, key, resource, registration);
        } catch (Exception e) {
            if (ops instanceof ContextualOps<?> c && c.catlib$hasContext()) {
                final var mod = ModDescriptor.forMod(key.location().getNamespace());
                LibErrorContext.error(mod, new DetailedDataLoadException(c.catlib$getContext(), key, e));
            }
            throw e;
        } finally {
            if (ops instanceof ContextualOps<?> c) {
                c.catlib$resetContext();
            }
        }
    }
}
