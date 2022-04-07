package personthecat.catlib.event;

import personthecat.catlib.data.collections.NonRecursiveObserverSet;
import personthecat.catlib.data.collections.ObserverSet;
import personthecat.catlib.data.collections.SimpleObserverSet;

import java.util.function.Function;

public class LibEvent<T> {
    private final ObserverSet<T> listeners;
    private final T invoker;
    private final T emptyInvoker;

    private LibEvent(final ObserverSet<T> listeners, final T invoker, final T emptyInvoker) {
        this.listeners = listeners;
        this.invoker = invoker;
        this.emptyInvoker = emptyInvoker;
    }

    public static <T> LibEvent<T> create(final Function<ObserverSet<T>, T> event) {
        return create(new SimpleObserverSet<>(), event);
    }

    public static <T> LibEvent<T> nonRecursive(final Function<ObserverSet<T>, T> event) {
        return create(new NonRecursiveObserverSet<>(), event);
    }

    private static <T> LibEvent<T> create(final ObserverSet<T> listeners, final Function<ObserverSet<T>, T> event) {
        final T invoker = event.apply(listeners);
        return new LibEvent<>(listeners, invoker, invoker);
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
