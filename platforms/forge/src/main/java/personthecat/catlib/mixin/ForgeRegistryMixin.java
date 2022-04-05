package personthecat.catlib.mixin;

import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.event.LibEvent;
import personthecat.catlib.registry.ForgeRegistryHandle;
import personthecat.catlib.event.registry.RegistryAddedCallback;
import personthecat.catlib.event.registry.RegistryEventAccessor;
import personthecat.catlib.registry.RegistryHandle;

@Mixin(ForgeRegistry.class)
public abstract class ForgeRegistryMixin<T extends IForgeRegistryEntry<T>> implements RegistryEventAccessor<T> {

    @Final
    @Shadow(remap = false)
    private RegistryManager stage;

    @Nullable
    private LibEvent<RegistryAddedCallback<T>> registryAddedEvent = null;

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Inject(method = "add(ILnet/minecraftforge/registries/IForgeRegistryEntry;Ljava/lang/String;)I", at = @At("RETURN"), remap = false)
    void onAdd(int id, T t, String owner, CallbackInfoReturnable<Integer> ci) {
        if (this.registryAddedEvent != null && this.stage == RegistryManager.ACTIVE ) {
            final RegistryHandle<T> handle = new ForgeRegistryHandle<>((ForgeRegistry<T>) (Object) this);
            this.registryAddedEvent.invoker().onRegistryAdded(handle, t.getRegistryName(), t);
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
