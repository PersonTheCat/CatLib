package personthecat.catlib.event.lifecycle;

import personthecat.catlib.event.LibEvent;

public class CheckErrorsEvent {
    public static final LibEvent<Runnable> EVENT =
        LibEvent.create(callbacks -> () -> callbacks.forEach(Runnable::run));
}
