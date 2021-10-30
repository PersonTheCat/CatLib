package personthecat.catlib.data;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NonRecursiveIterable<E> implements Collection<E> {

    private final Collection<E> wrapped;
    private final Set<WeakReference<NonRecursiveIterator>> active;

    public NonRecursiveIterable(final Collection<E> wrapped) {
        this.wrapped = wrapped;
        this.active = new HashSet<>();
    }

    @Override
    public int size() {
        return this.wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return this.wrapped.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return this.wrapped.contains(o);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        final NonRecursiveIterator iterator = new NonRecursiveIterator();
        this.active.add(new WeakReference<>(iterator));
        return iterator;
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return this.wrapped.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return this.wrapped.toArray(a);
    }

    @Override
    public boolean add(final E e) {
        return this.wrapped.add(e);
    }

    @Override
    public boolean remove(final Object o) {
        return this.wrapped.remove(o);
    }

    @Override
    public boolean containsAll(final @NotNull Collection<?> c) {
        return this.wrapped.containsAll(c);
    }

    @Override
    public boolean addAll(final @NotNull Collection<? extends E> c) {
        return this.wrapped.addAll(c);
    }

    @Override
    public boolean removeAll(final @NotNull Collection<?> c) {
        return this.wrapped.removeAll(c);
    }

    @Override
    public boolean retainAll(final @NotNull Collection<?> c) {
        return this.wrapped.retainAll(c);
    }

    @Override
    public void clear() {
        this.wrapped.clear();
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof NonRecursiveIterable && this.wrapped.equals(((NonRecursiveIterable<?>) o).wrapped);
    }

    @Override
    public int hashCode() {
        return this.wrapped.hashCode();
    }

    private class NonRecursiveIterator implements Iterator<E> {
        final Iterator<E> iterator = wrapped.iterator();
        int index = 0;

        @Override
        public boolean hasNext() {
            return this.index < NonRecursiveIterable.this.size();
        }

        @Override
        public E next() {
            this.index++;
            if (active.size() == 1) {
                return this.iterator.next();
            }
            E next = this.iterator.next();
            if (this.iterator.hasNext()) {
                if (this.isActive()) {
                    next = this.iterator.next();
                    this.index++;
                }
            } else {
                active.remove(new WeakReference<>(this));
            }
            return next;
        }

        private boolean isActive() {
            final Iterator<WeakReference<NonRecursiveIterator>> activeIterators = active.iterator();
            while (activeIterators.hasNext()) {
                final NonRecursiveIterator other = activeIterators.next().get();
                if (other != null) {
                    if (other != this && other.index == this.index) {
                        return true;
                    }
                } else {
                    activeIterators.remove();
                }
            }
            return false;
        }

        @Override
        public void remove() {
            this.iterator.remove();
        }
    }
}
