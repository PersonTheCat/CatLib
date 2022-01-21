package personthecat.catlib.util;

import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DimInjector {
    void setType(final @NotNull DimensionType type);
    @Nullable DimensionType getType();
}
