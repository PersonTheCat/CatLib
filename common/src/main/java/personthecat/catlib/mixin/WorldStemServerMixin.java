package personthecat.catlib.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.WorldStem;
import net.minecraft.server.WorldStem.WorldDataSupplier;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.lifecycle.GameReadyEvent;

@Mixin(WorldStem.class)
public class WorldStemServerMixin {

    @ModifyVariable(method = "load", at = @At(value = "HEAD"), index = 2)
    private static WorldDataSupplier injectCatlibEvent(final WorldDataSupplier supplier) {
        return (manager, config) -> {
            final Pair<WorldData, RegistryAccess.Frozen> ret = supplier.get(manager, config);
            modSetupComplete();
            return ret;
        };
    }

    private static void modSetupComplete() {
        LibErrorContext.outputServerErrors(true);
        GameReadyEvent.SERVER.invoker().run();
    }
}
