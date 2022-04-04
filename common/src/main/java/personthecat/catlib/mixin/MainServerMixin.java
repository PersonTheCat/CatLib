package personthecat.catlib.mixin;

import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.lifecycle.GameReadyEvent;

@Mixin(Main.class)
public class MainServerMixin {
// This may have to go to WorldStem.load()
    @Inject(method = "main", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/RegistryAccess;builtinCopy()Lnet/minecraft/core/RegistryAccess$Writable;"))
    private static void modSetupComplete(final CallbackInfo ci) {
        LibErrorContext.outputServerErrors(true);
        GameReadyEvent.SERVER.invoker().run();
    }
}
