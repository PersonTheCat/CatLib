package personthecat.catlib.event;

import personthecat.catlib.data.NonRecursiveObserverSet;
import personthecat.catlib.data.ObserverSet;
import personthecat.catlib.data.SimpleObserverSet;

import java.util.function.Function;

public class LibEvent<T> {
    private final ObserverSet<T> listeners;
    private final T invoker;

    private LibEvent(final ObserverSet<T> listeners, final T invoker) {
        this.listeners = listeners;
        this.invoker = invoker;
    }

    public static <T> LibEvent<T> create(final Function<ObserverSet<T>, T> event) {
        return create(new SimpleObserverSet<>(), event);
    }

    public static <T> LibEvent<T> nonRecursive(final Function<ObserverSet<T>, T> event) {
        return create(new NonRecursiveObserverSet<>(), event);
    }

    private static <T> LibEvent<T> create(final ObserverSet<T> listeners, final Function<ObserverSet<T>, T> event) {
        return new LibEvent<>(listeners, event.apply(listeners));
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

    public void deregister(final T listener) {
        this.listeners.remove(listener);
    }

    public T invoker() {
        return this.invoker;
    }

    public boolean isEmpty() {
        return this.listeners.isEmpty();
    }

    public void clear() {
        this.listeners.clear();
    }
}
