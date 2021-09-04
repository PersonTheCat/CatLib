package personthecat.catlib.mixin;

import net.minecraft.resources.ResourceLocation;
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
import personthecat.catlib.event.registry.RegistryAddedCallback;
import personthecat.catlib.event.registry.RegistryEventAccessor;
import personthecat.catlib.event.registry.RegistryHandle;
import personthecat.catlib.exception.MissingOverrideException;

@Mixin(ForgeRegistry.class)
public class ForgeRegistryMixin<T extends IForgeRegistryEntry<T>> implements RegistryHandle<T>, RegistryEventAccessor<T> {

    @Nullable
    private LibEvent<RegistryAddedCallback<T>> registryAddedEvent = null;

    @Final
    @Shadow(remap = false)
    private RegistryManager stage;

    @Inject(method = "add(ILnet/minecraftforge/registries/IForgeRegistryEntry;Ljava/lang/String;)I", at = @At("RETURN"), remap = false)
    void onAdd(int id, T t, String owner, CallbackInfoReturnable<Integer> ci) {
        if (this.registryAddedEvent != null && this.stage == RegistryManager.ACTIVE ) {
            this.registryAddedEvent.invoker().onRegistryAdded(this, t.getRegistryName(), t);
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

    @Nullable
    @Override
    public T lookup(final ResourceLocation id) {
        return this.getValue(id);
    }

    @Override
    public void register(final ResourceLocation id, final T t) {
        this.register(t.setRegistryName(id));
    }

    @Shadow(remap = false)
    public T getValue(final ResourceLocation id) {
        throw new MissingOverrideException();
    }

    @Shadow(remap = false)
    public void register(final T t) {
        throw new MissingOverrideException();
    }
}
