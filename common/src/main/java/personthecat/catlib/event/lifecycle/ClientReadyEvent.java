package personthecat.catlib.event.lifecycle;

import personthecat.catlib.event.LibEvent;

public class ClientReadyEvent {

    public static final LibEvent<Runnable> EVENT =
        LibEvent.create(callbacks -> () -> callbacks.forEach(Runnable::run));
}
