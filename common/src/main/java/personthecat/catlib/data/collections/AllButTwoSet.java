package personthecat.catlib.data.collections;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AllButTwoSet<T> extends AbstractSet<T> {

    private final Set<T> wrapped;
    private final T one;
    private final T two;

    public AllButTwoSet(final Collection<T> all, final T one, final T two) {
        this.wrapped = new HashSet<>(all);
        this.wrapped.remove(one);
        this.wrapped.remove(two);
        this.one = one;
        this.two = two;
    }

    @Override
    public int size() {
        return this.wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return !(this.one.equals(o) || this.two.equals(o));
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
    public <T1> T1[] toArray(@NotNull T1[] a) {
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
    public boolean containsAll(@NotNull Collection<?> c) {
        for (final Object o : c) {
            if (this.one.equals(o) || this.two.equals(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {}
}
