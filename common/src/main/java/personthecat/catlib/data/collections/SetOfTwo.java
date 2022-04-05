package personthecat.catlib.data.collections;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public class SetOfTwo<T> extends AbstractSet<T> {

    private final T one;
    private final T two;

    public SetOfTwo(final T one, final T two) {
        this.one = one;
        this.two = two;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(final Object o) {
        return this.one.equals(o) || this.two.equals(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new IteratorOfTwo();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return new Object[] { this.one, this.two };
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public <T1> T1[] toArray(@NotNull T1[] a) {
        if (a.length != 2) {
            a = (T1[]) Array.newInstance(a.getClass().getComponentType(), 2);
        }
        a[0] = (T1) this.one;
        a[1] = (T1) this.two;
        return a;
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
        return c.contains(this.one) && c.contains(this.two);
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

    private class IteratorOfTwo implements Iterator<T> {
        int index = 0;

        @Override
        public boolean hasNext() {
            return this.index < 2;
        }

        @Override
        public T next() {
            int i = this.index++;
            if (i == 0) return one;
            return i == 1 ? two : null;
        }
    }
}
