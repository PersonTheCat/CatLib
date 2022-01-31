package personthecat.catlib.data;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class InfinitySet<T> extends AbstractSet<T> {

    private final Set<T> wrapped;

    public InfinitySet(final Collection<T> all) {
        this.wrapped = new HashSet<>(all);
    }

    @Override
    public int size() {
        return wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(final Object o) {
        return true;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this.wrapped.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return this.wrapped.toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(final @NotNull T1[] a) {
        return this.wrapped.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(final @NotNull Collection<?> c) {
        return true;
    }

    @Override
    public boolean addAll(final @NotNull Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean retainAll(final @NotNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(final @NotNull Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {}
}
