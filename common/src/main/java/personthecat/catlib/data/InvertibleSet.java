package personthecat.catlib.data;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

/**
 * This object is a wrapper for any other {@link Set} implementor. It provides
 * an additional method, {@link #check}, which will return the opposite value
 * when {@link #blacklist} is enabled. <b>All other methods are unchanged.</b>
 *
 * @param <T> The type of value in the collection.
 */
@Immutable
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class InvertibleSet<T> implements Set<T> {

    private final boolean blacklist;
    private final Set<T> set;

    public InvertibleSet(final Set<T> set, final boolean blacklist) {
        this.set = Collections.unmodifiableSet(set);
        this.blacklist = blacklist;
    }

    public static <T> InvertibleSet<T> wrap(final Set<T> set) {
        return new InvertibleSet<>(set, false);
    }

    public InvertibleSet<T> blacklist(final boolean b) {
        return new InvertibleSet<>(this.set, b);
    }

    public boolean isBlacklist() {
        return this.blacklist;
    }

    public <U> boolean check(final Function<T, U> mapper, final U u2) {
        return this.blacklist != this.set.stream().map(mapper).anyMatch(u1 -> u1.equals(u2));
    }

    @Override
    public int size() {
        return this.set.size();
    }

    @Override
    public boolean isEmpty() {
        return this.set.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return this.set.contains(o);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return this.set.containsAll(c);
    }

    @Override
    public Iterator<T> iterator() {
        return this.set.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.set.toArray();
    }

    @Override
    public <T1> T1[] toArray(final T1[] a) {
        return this.set.toArray(a);
    }

    @Override
    @Deprecated
    public boolean add(final T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean addAll(final Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
