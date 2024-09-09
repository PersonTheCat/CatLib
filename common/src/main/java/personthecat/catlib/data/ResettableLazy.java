package personthecat.catlib.data;

import org.jetbrains.annotations.NotNull;
import oshi.annotation.concurrent.ThreadSafe;

import java.util.function.Supplier;

/**
 * Variant of {@link Lazy} which is allowed to be reset.
 * <p>
 *   In addition to being reloadable, this wrapper is thread-safe and provides a guaranteed
 *   method for retrieving up-to-date values: {@link #getUpdated()}.
 * </p>
 * @param <T> The type of value being consumed by the wrapper.
 */
@ThreadSafe
@SuppressWarnings("UnusedReturnValue")
public class ResettableLazy<T> extends Lazy<T> {

    /**
     * The primary constructor with instructions for producing the value.
     *
     * @param supplier A function used for generating the value, when ready.
     */
    public ResettableLazy(@NotNull Supplier<T> supplier) {
        super(supplier);
    }

    /**
     * To be used in the event that a value already exists.
     *
     * @param value The actual value being wrapped.
     */
    public ResettableLazy(@NotNull T value) {
        super(value);
    }

    /**
     * Factory variant of {@link #ResettableLazy(Supplier)}.
     *
     * @param <T> The type of value being wrapped.
     * @param supplier A supplier providing this value, when the time comes.
     * @return The value, to be calculated on first use.s
     */
    public static <T> ResettableLazy<T> of(@NotNull final Supplier<T> supplier) {
        return new ResettableLazy<>(supplier);
    }

    /**
     * Factory variant of {@link #ResettableLazy(Object)}.
     *
     * @param <T> The type of value being wrapped.
     * @param value The actual value being wrapped.
     * @return The value, consumed by the wrapper.
     */
    public static <T> ResettableLazy<T> of(@NotNull final T value) {
        return new ResettableLazy<>(value);
    }

    /**
     * Converts this wrapper into a resettable or non-resettable value.
     *
     * @param resettable Whether the wrapper should be resettable.
     * @return Either <code>this</code> or a regular {@link Lazy}.
     */
    public Lazy<T> asResettable(final boolean resettable) {
        if (resettable) {
            return this;
        }
        return this.set ? new Lazy<>(this.value) : new Lazy<>(this.supplier);
    }

    /**
     * Marks this object as being uninitialized. It will be loaded again on next use.
     *
     * @return <code>this</code>, for method chaining.
     */
    public synchronized ResettableLazy<T> reset() {
        this.set = false;
        this.value = null;
        return this;
    }

    /**
     * Returns whether the underlying data can be reloaded.
     *
     * @return <code>true</code>, always.
     */
    @Override
    public boolean isResettable() {
        return true;
    }
}
