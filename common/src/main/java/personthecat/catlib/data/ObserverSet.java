package personthecat.catlib.data;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Variant of {@link Collection} supporting concurrent modifications.
 *
 * <p>Note that concurrent modification is only guaranteed for removals,
 * not add operations. Any ongoing <code>forEach</code> operations will
 * be immediately aware of any listeners having been removed.
 *
 * @param <O> The type of observer contained in this collection.
 */
public interface ObserverSet<O> {

    /**
     * Gets the number of listeners in this collection.
     *
     * @return The number of listeners.
     */
    int size();

    /**
     * Adds another listener to this collection.
     *
     * @param o The listener being added.
     */
    void add(final O o);

    /**
     * Indicates whether this listener currently exists in the collection.
     *
     * @param o The listener being compared against.
     * @return <code>true</code> if the listener is registered.
     */
    boolean contains(final O o);

    /**
     * Removes a listener from the collection. Note that, unlike {@link Collection#remove},
     * this implementation is expected to take effect immediately. Any ongoing iterations
     * over this collection will be immediately aware of the change.
     *
     * @param o The listener being removed.
     */
    void remove(final O o);

    /**
     * Removes every listener from this collection. Note that, unlike {@link Collection#clear},
     * this implementation is expected to take effect immediately. Any ongoing iterations
     * over this collection will immediately break.
     */
    void clear();

    /**
     * Converts this collection into an untracked {@link Collection} of the same type.
     *
     * <p>In the event where the underlying data in this collection are updated, the
     * collection returned by this method will be unaware.
     *
     * @return A <b>new</b> collection containing the same listeners.
     */
    Collection<O> getUntracked();

    /**
     * The only way to iterate over this collection. This controlled exposure is what
     * enables <code>ObserverSet</code> to support concurrent modification.
     *
     * @param fn The action to perform on each listener in the collection.
     */
    void forEach(final Consumer<O> fn);

    /**
     * Indicates whether any listeners are present in the collection at all.
     *
     * @return <code>true</code> if no listeners are present in the collection.
     */
    default boolean isEmpty() {
        return this.size() == 0;
    }

    /**
     * Adds every listener from the given collection into this object.
     *
     * @param collection A collection of listeners being added.
     */
    default void addAll(final Collection<O> collection) {
        for (final O o : collection) this.add(o);
    }

    /**
     * Determines whether this object contains every listener in the given collection.
     *
     * @param collection A collection of listeners being compared against.
     * @return <code>true</code> only if this object contains every listener.
     */
    default boolean containsAll(final Collection<O> collection) {
        for (final O o : collection) {
            if (!this.contains(o)) return false;
        }
        return true;
    }

    /**
     * Removes every listener from the given collection into this object.
     *
     * @param collection A collection of listeners being removed.
     */
    default void removeAll(final Collection<O> collection) {
        for (final O o : collection) this.remove(o);
    }

    /**
     * The backbone of every <code>ObserverSet</code> implementation. These entries
     * are used to attach state-like metadata to the observers in this collection.
     * This enables the object to effectively "remove" entries at a global scale,
     * even if the underlying collection has been cloned.
     *
     * @param <O> The type of observer contained within this collection.
     */
    interface TrackedEntry<O> {

        /**
         * Indicates whether this entry is currently being operated on.
         *
         * @return <code>true</code>, if this entry is in-use.
         */
        boolean isActive();

        /**
         * Indicates whether this entry has been effectively removed.
         *
         * <p>Note that the default implementations of this interface are only used
         * when this observer has also been removed from the source collection.
         *
         * @return <code>true</code> if this entry was removed from the source.
         */
        boolean isRemoved();

        /**
         * Flags this entry as having been removed from the source collection.
         */
        void remove();

        /**
         * Exposes the raw observer being wrapped.
         *
         * @return The raw observer.
         */
        O getObserver();
    }
}
