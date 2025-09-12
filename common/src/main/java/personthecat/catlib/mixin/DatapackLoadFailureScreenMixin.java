package personthecat.catlib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.catlib.client.gui.LibErrorMenu;
import personthecat.catlib.event.error.LibErrorContext;

import java.util.Objects;

@Mixin(DatapackLoadFailureScreen.class)
public abstract class DatapackLoadFailureScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void postInit(CallbackInfo ci) {
        if (LibErrorContext.hasErrors()) {
            final var mc = Objects.requireNonNull(Minecraft.getInstance());
            mc.setScreen(new LibErrorMenu((Screen) (Object) this));
        }
    }
}
