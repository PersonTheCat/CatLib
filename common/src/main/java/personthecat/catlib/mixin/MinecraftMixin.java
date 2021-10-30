package personthecat.catlib.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.catlib.event.lifecycle.ClientReadyEvent;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    private static final AtomicBoolean INIT_COMPLETE = new AtomicBoolean(false);

    @Inject(method = "setScreen", at = @At("HEAD"))
    public void postInit(final CallbackInfo ci) {
        if (INIT_COMPLETE.compareAndSet(false, true)) {
            ClientReadyEvent.EVENT.invoker().run();
        }
    }
}
