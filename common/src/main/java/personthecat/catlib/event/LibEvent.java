package personthecat.catlib.event;

import personthecat.catlib.data.NonRecursiveIterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class LibEvent<T> {
    private final Function<Collection<T>, T> event;
    private final Collection<T> listeners;
    private volatile T invoker;

    private LibEvent(final Function<Collection<T>, T> event, final Collection<T> listeners) {
        this.event = event;
        this.listeners = listeners;
        this.invoker = null;
    }

    public static <T> LibEvent<T> create(final Function<Collection<T>, T> event) {
        return new LibEvent<>(event, Collections.synchronizedList(new ArrayList<>()));
    }

    public static <T> LibEvent<T> nonRecursive(final Function<Collection<T>, T> event) {
        return new LibEvent<>(event, Collections.synchronizedCollection(new NonRecursiveIterable<>(new ArrayList<>())));
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
            this.invoker = this.listeners.iterator().next();
        } else {
            this.invoker = this.event.apply(this.listeners);
        }
        return this;
    }

    public boolean isEmpty() {
        return this.listeners.isEmpty();
    }
}
