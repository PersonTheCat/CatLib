package personthecat.catlib.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(HolderSet.Named.class)
public interface NamedHolderSetAccessor<T> {

    @Accessor
    List<Holder<T>> getContents();
}
