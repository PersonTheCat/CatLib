package personthecat.catlib.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Set;

@Mixin(HolderSet.Direct.class)
public interface DirectHolderSetAccessor<T> {

    @Accessor
    List<Holder<T>> getContents();

    @Mutable
    @Accessor
    void setContents(final List<Holder<T>> holders);

    @Accessor
    void setContentsSet(final @Nullable Set<Holder<T>> holders);
}
