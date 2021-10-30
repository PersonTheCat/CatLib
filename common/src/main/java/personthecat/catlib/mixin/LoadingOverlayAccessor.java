package personthecat.catlib.mixin;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public interface LoadingOverlayAccessor {

    @Accessor
    Consumer<Optional<Throwable>> getOnFinish();

    @Mutable
    @Accessor
    void setOnFinish(final Consumer<Optional<Throwable>> onFinish);
}
