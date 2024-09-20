package personthecat.catlib.event.world;

import net.minecraft.core.Registry;
import net.minecraft.world.level.dimension.LevelStem;
import personthecat.catlib.event.LibEvent;

import java.util.function.Consumer;

public class DimensionBakeEvent {
    public static final LibEvent<Consumer<Registry<LevelStem>>> EVENT =
        LibEvent.create(callbacks -> registry -> callbacks.forEach(c -> c.accept(registry)));
}
