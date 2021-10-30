package personthecat.catlib.event.player;

import personthecat.catlib.event.LibEvent;

public class CommonPlayerEvent {

    public static final LibEvent<PlayerConnectedCallback> LOGIN =
        LibEvent.create(callbacks -> (p, s) -> callbacks.forEach(callback -> callback.accept(p, s)));

    public static final LibEvent<PlayerConnectedCallback> LOGOUT =
        LibEvent.create(callbacks -> (p, s) -> callbacks.forEach(callback -> callback.accept(p, s)));
}
