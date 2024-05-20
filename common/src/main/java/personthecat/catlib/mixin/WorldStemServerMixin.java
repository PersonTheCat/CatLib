package personthecat.catlib.mixin;

import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldLoader.DataLoadOutput;
import net.minecraft.server.WorldLoader.WorldDataSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.lifecycle.GameReadyEvent;

@Mixin(WorldLoader.class)
public class WorldStemServerMixin {

    @ModifyArg(method = "load", at = @At(value = "HEAD"), index = 1)
    private static <D> WorldDataSupplier<D> injectCatlibEvent(final WorldDataSupplier<D> supplier) {
        return (ctx) -> {
            final DataLoadOutput<D> ret = supplier.get(ctx);
            modSetupComplete();
            return ret;
        };
    }

    private static void modSetupComplete() {
        LibErrorContext.outputServerErrors(true);
        GameReadyEvent.SERVER.invoker().run();
    }
}
