package personthecat.catlib.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class LibEvent<T> {
    private final Function<List<T>, T> event;
    private final List<T> listeners;
    private volatile T invoker;

    private LibEvent(final Function<List<T>, T> event) {
        this.event = event;
        this.listeners = Collections.synchronizedList(new ArrayList<>());
        this.invoker = null;
    }

    public static <T> LibEvent<T> create(final Function<List<T>, T> event) {
        return new LibEvent<>(event);
    }

    public LibEvent<T> register(final T listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
        return this;
    }

    public boolean isRegistered(final T listener) {
        return this.listeners.contains(listener);
    }

    public boolean deregister(final T listener) {
        return this.listeners.remove(listener);
    }

    public T invoker() {
        if (this.invoker != null) {
            return this.invoker;
        }
        return this.update().invoker;
    }

    private synchronized LibEvent<T> update() {
        if (this.listeners.size() == 1) {
            this.invoker = listeners.get(0);
        } else {
            this.invoker = this.event.apply(this.listeners);
        }
        return this;
    }

    public boolean isEmpty() {
        return this.listeners.isEmpty();
    }
}
