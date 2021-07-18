package personthecat.catlib.data;

import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.util.function.Supplier;

/**
 * Variant of {@link Lazy} which is allowed to be reset.
 * <p>
 *   In addition to being reloadable, this wrapper is threadsafe and provides a guaranteed
 *   method for retrieving up-to-date values: {@link #getUpdated()}.
 * </p>
 * @param <T> The type of value being consumed by the wrapper.
 */
@ThreadSafe
@SuppressWarnings("unused")
public class ResettableLazy<T> extends Lazy<T> {

    /** Whether the value has been setup. */
    private volatile boolean set;

    /** The primary constructor with instructions for producing the value. */
    ResettableLazy(@NotNull Supplier<T> supplier) {
        super(supplier);
        this.set = false;
    }

    /** To be used in the event that a value already exists. */
    ResettableLazy(@NotNull T value) {
        super(value);
        this.set = true;
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
     * Factory variant of {@link #ResettableLazy(T)}.
     *
     * @param <T> The type of value being wrapped.
     * @param value The actual value being wrapped.
     * @return The value, consumed by the wrapper.
     */
    public static <T> ResettableLazy<T> of(@NotNull final T value) {
        return new ResettableLazy<>(value);
    }

    /** The primary method for retrieving the underlying value. */
    @NotNull
    @Override
    public T get() {
        if (!this.set) {
            synchronized(this) {
                this.value = this.supplier.get();
                this.set = true;
            }
        }
        return value;
    }

    /** Marks this object as being uninitialized. It will be loaded again on next use. */
    public synchronized ResettableLazy<T> reset() {
        this.set = false;
        this.value = null;
        return this;
    }

    /** Returns an up-to-date value without resetting this value's reference. */
    public T getUpdated() {
        return this.supplier.get();
    }
}
