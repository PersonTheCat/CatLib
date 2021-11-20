package personthecat.catlib.mixin;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.catlib.event.lifecycle.GameReadyEvent;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    private static final AtomicBoolean INIT_COMPLETE = new AtomicBoolean(false);

    @Inject(method = "init", at = @At("TAIL"))
    public void postInit(final CallbackInfo ci) {
        if (INIT_COMPLETE.compareAndSet(false, true)) {
            GameReadyEvent.CLIENT.invoker().run();
        }
    }
}
