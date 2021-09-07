package personthecat.catlib.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.event.LibEvent;
import personthecat.catlib.event.registry.MojangRegistryHandle;
import personthecat.catlib.event.registry.RegistryAddedCallback;
import personthecat.catlib.event.registry.RegistryEventAccessor;

@Mixin(MappedRegistry.class)
public abstract class MojangRegistryMixin<T> extends Registry<T> implements RegistryEventAccessor<T> {

    @Nullable
    private LibEvent<RegistryAddedCallback<T>> registryAddedEvent = null;

    protected MojangRegistryMixin(final ResourceKey<? extends Registry<T>> key, final Lifecycle lifecycle) {
        super(key, lifecycle);
    }

    @Inject(method = "register", at = @At("RETURN"))
    public <V extends T> void onRegister(ResourceKey<T> key, V v, Lifecycle lifecycle, CallbackInfoReturnable<V> ci) {
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

