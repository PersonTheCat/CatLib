package personthecat.catlib.mixin.forge;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.event.LibEvent;
import personthecat.catlib.registry.MojangRegistryHandle;
import personthecat.catlib.event.registry.RegistryAddedCallback;
import personthecat.catlib.event.registry.forge.RegistryEventAccessor;

@Mixin(MappedRegistry.class)
public abstract class MojangRegistryMixin<T> implements Registry<T>, RegistryEventAccessor<T> {

    @Unique
    @Nullable
    private LibEvent<RegistryAddedCallback<T>> registryAddedEvent = null;

    @Inject(method = "register", at = @At("RETURN"))
    public <V extends T> void onRegister(ResourceKey<T> key, V v, RegistrationInfo info, CallbackInfoReturnable<Holder.Reference<V>> cir) {
        if (this.registryAddedEvent != null) {
            this.registryAddedEvent.invoker().onRegistryAdded(new MojangRegistryHandle<>(this), key.location(), v);
        }
    }

    @NotNull
    @Override
    public LibEvent<RegistryAddedCallback<T>> getRegistryAddedEvent() {
        if (this.registryAddedEvent == null) {
            this.registryAddedEvent = LibEvent.create(callbacks -> (handle, id, t) ->
                callbacks.forEach(c -> c.onRegistryAdded(handle, id, t)));
        }
        return this.registryAddedEvent;
    }
}

