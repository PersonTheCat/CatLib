package personthecat.catlib.mixin;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.catlib.event.registry.RegistryAccessEvent;
import personthecat.catlib.event.world.BiomeCacheInvalidator;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Final
    @Shadow
    protected WorldData worldData;

    @Shadow
    public abstract RegistryAccess.Frozen registryAccess();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(final CallbackInfo ci) {
        RegistryAccessEvent.EVENT.invoker().accept(this.registryAccess());
        BiomeCacheInvalidator.invalidateBiomes(this.worldData);
    }
}
