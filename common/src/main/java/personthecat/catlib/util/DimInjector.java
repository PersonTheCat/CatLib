package personthecat.catlib.util;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.event.registry.DynamicRegistries;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public interface DimInjector {
    Set<Class<?>> UNSUPPORTED = ConcurrentHashMap.newKeySet();
    Set<DimensionType> MAPPED = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
    Logger LOG = LogManager.getLogger(DimInjector.class);

    void setType(final @NotNull DimensionType type);
    @Nullable DimensionType getType();

    static void setType(final ChunkAccess chunk, final @NotNull DimensionType type) {
        if (chunk instanceof DimInjector) {
            ((DimInjector) chunk).setType(type);
            if (MAPPED.add(type)) {
                LOG.info("Successfully injecting dim keys for {}", DynamicRegistries.DIMENSION_TYPES.getKey(type));
            }
        } else if (UNSUPPORTED.add(chunk.getClass())) {
            LOG.error("Cannot inject into chunk of type {}", chunk.getClass());
        }
    }
}
