package personthecat.catlib.event.lifecycle;

import personthecat.catlib.event.LibEvent;
import personthecat.catlib.util.McUtils;

public class GameReadyEvent {

    public static final LibEvent<Runnable> CLIENT =
        LibEvent.create(callbacks -> () -> callbacks.forEach(Runnable::run));

    public static final LibEvent<Runnable> SERVER =
        LibEvent.create(callbacks -> () -> callbacks.forEach(Runnable::run));

    public static final LibEvent<Runnable> COMMON =
        McUtils.isClientSide() ? CLIENT : SERVER;
}
