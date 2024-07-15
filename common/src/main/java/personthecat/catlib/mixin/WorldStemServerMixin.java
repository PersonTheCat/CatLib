package personthecat.catlib.mixin;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader.DataLoadOutput;
import net.minecraft.server.WorldLoader.WorldDataSupplier;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.lifecycle.GameReadyEvent;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(WorldStem.class)
public class WorldStemServerMixin {
    private static final AtomicBoolean INIT_COMPLETE = new AtomicBoolean(false);

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void injectCatlibEvent(
            CloseableResourceManager resourceManager, ReloadableServerResources dataPackResources,
            LayeredRegistryAccess<RegistryLayer> registries, WorldData worldData, CallbackInfo ci) {
        if (INIT_COMPLETE.compareAndSet(false, true)) {
            LibErrorContext.outputServerErrors(true);
            GameReadyEvent.SERVER.invoker().run();
        }
    }
}
