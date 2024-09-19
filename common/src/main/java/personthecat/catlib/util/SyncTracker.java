package personthecat.catlib.util;

import net.minecraft.resources.ResourceKey;

public interface SyncTracker<T> {
    void markUpdated(ResourceKey<T> key);
}
