package personthecat.catlib.event.world;

import personthecat.catlib.event.LibEvent;

import java.util.function.Consumer;

public class FeatureModificationEvent {
    public static final LibEvent<Consumer<FeatureModificationContext>> EVENT =
        LibEvent.create(callbacks -> ctx -> callbacks.forEach(c -> c.accept(ctx)));
}
