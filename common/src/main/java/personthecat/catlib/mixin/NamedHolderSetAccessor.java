package personthecat.catlib.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(HolderSet.Named.class)
public interface NamedHolderSetAccessor<T> {

    @Accessor
    List<Holder<T>> getContents();

    @Invoker
    void invokeBind(final List<Holder<T>> holders);
}
