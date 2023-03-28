package personthecat.catlib.mixin.forge;

import com.mojang.datafixers.util.Pair;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.server.Main;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.event.lifecycle.GameReadyEvent;

@Mixin(Main.class)
public class MainServerMixin {

    @Inject(
        method = "m_206537_",
        at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/RegistryAccess;builtinCopy()Lnet/minecraft/core/RegistryAccess$Writable;"))
    private static void modSetupComplete(
            LevelStorageSource.LevelStorageAccess a, OptionSet o, OptionSpec<?> s, DedicatedServerSettings c,
            OptionSpec<?> optionSpec2, ResourceManager r, DataPackConfig d, CallbackInfoReturnable<Pair<?, ?>> cir) {
        LibErrorContext.outputServerErrors(true);
        GameReadyEvent.SERVER.invoker().run();
    }
}
