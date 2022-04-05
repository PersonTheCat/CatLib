package personthecat.catlib.data.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class SimpleObserverSet<O> implements ObserverSet<O> {

    protected final List<SimpleTrackedEntry<O>> tracked;

    public SimpleObserverSet() {
        this.tracked = new ArrayList<>();
    }

    public SimpleObserverSet(final Collection<O> entries) {
        this.tracked = beginTracking(entries);
    }

    private static <O> List<SimpleTrackedEntry<O>> beginTracking(final Collection<O> entries) {
        final List<SimpleTrackedEntry<O>> tracked = new ArrayList<>();
        for (final O o : entries) {
            tracked.add(new SimpleTrackedEntry<>(o));
        }
        return tracked;
    }

    @Override
    public int size() {
        return this.tracked.size();
    }

    @Override
    public synchronized void add(final O o) {
        this.tracked.add(new SimpleTrackedEntry<>(o));
    }

    @Override
    public boolean contains(final O o) {
        for (final SimpleTrackedEntry<O> entry : this.tracked) {
            if (!entry.isRemoved() && entry.getObserver().equals(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void remove(final O o) {
        final Iterator<SimpleTrackedEntry<O>> iterator = this.tracked.iterator();
        while (iterator.hasNext()) {
            final SimpleTrackedEntry<O> entry = iterator.next();
            if (entry.getObserver().equals(o)) {
                entry.remove();
                iterator.remove();
                return;
            }
        }
    }

    @Override
    public synchronized void clear() {
        for (final SimpleTrackedEntry<O> entry : this.tracked) {
            entry.remove();
        }
        this.tracked.clear();
    }

    @Override
    public Collection<O> getUntracked() {
        final List<O> untracked = new ArrayList<>();
        for (final SimpleTrackedEntry<O> entry : this.tracked) {
            untracked.add(entry.getObserver());
        }
        return untracked;
    }

    @Override
    public void forEach(final Consumer<O> fn) {
        for (final SimpleTrackedEntry<O> entry : new ArrayList<>(this.tracked)) {
            if (!entry.isRemoved()) {
                fn.accept(entry.getObserver());
            }
        }
    }

    protected static class SimpleTrackedEntry<O> implements TrackedEntry<O> {
        volatile boolean active = false;
        volatile boolean removed = false;
        final O observer;

        SimpleTrackedEntry(final O observer) {
            this.observer = observer;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }

        protected void setActive(final boolean active) {
            this.active = active;
        }

        @Override
        public boolean isRemoved() {
            return this.removed;
        }

        @Override
        public void remove() {
            this.removed = true;
        }

        @Override
        public O getObserver() {
            return this.observer;
        }
    }
}
