package personthecat.catlib.mixin.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.event.LibEvent;
import personthecat.catlib.registry.forge.ForgeRegistryHandle;
import personthecat.catlib.event.registry.RegistryAddedCallback;
import personthecat.catlib.event.registry.forge.RegistryEventAccessor;
import personthecat.catlib.registry.RegistryHandle;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ForgeRegistry.class)
public abstract class ForgeRegistryMixin<T> implements RegistryEventAccessor<T>, IForgeRegistry<T> {

    @Final
    @Shadow(remap = false)
    private RegistryManager stage;

    @Unique
    @Nullable
    private LibEvent<RegistryAddedCallback<T>> registryAddedEvent = null;

    @Inject(method = "add(ILnet/minecraft/resources/ResourceLocation;Ljava/lang/Object;Ljava/lang/String;)I", at = @At("RETURN"), remap = false)
    void onAdd(int id, ResourceLocation key, T t, String owner, CallbackInfoReturnable<Integer> cir) {
        if (this.registryAddedEvent != null && this.stage == RegistryManager.ACTIVE ) {
            final RegistryHandle<T> handle = new ForgeRegistryHandle<>(this);
            final ResourceLocation location = handle.getKey(t);
            this.registryAddedEvent.invoker().onRegistryAdded(handle, location, t);
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
