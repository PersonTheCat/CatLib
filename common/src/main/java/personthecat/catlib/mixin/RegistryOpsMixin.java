package personthecat.catlib.mixin;

import net.minecraft.resources.RegistryOps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.serialization.codec.context.ContextualOps;
import personthecat.catlib.serialization.codec.context.DecodeContext;

@Mixin(RegistryOps.class)
public abstract class RegistryOpsMixin<T> implements ContextualOps<T> {
    @Unique
    private volatile DecodeContext catlib$context = new DecodeContext();

    @SuppressWarnings("unchecked")
    @Inject(method = "withParent", at = @At("RETURN"))
    public <U> void copyContext(CallbackInfoReturnable<RegistryOps<U>> cir) {
        ((RegistryOpsMixin<T>) (Object) cir.getReturnValue()).catlib$context = this.catlib$context;
    }

    @Override
    public DecodeContext catlib$getContext() {
        return this.catlib$context;
    }

    @Override
    public void catlib$resetContext() {
        this.catlib$context = new DecodeContext();
    }
}
