package personthecat.catlib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.client.gui.screen.LoadingErrorScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.catlib.event.lifecycle.GameReadyEvent;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    private static final AtomicBoolean INIT_COMPLETE = new AtomicBoolean(false);

    @Inject(method = "setScreen", at = @At("HEAD"))
    public void postInit(final @Nullable Screen screen, final CallbackInfo ci) {
        if (INIT_COMPLETE.compareAndSet(false, true)) {
            if (!(screen instanceof LoadingErrorScreen)) {
                GameReadyEvent.CLIENT.invoker().run();
            }
        }
    }
}
