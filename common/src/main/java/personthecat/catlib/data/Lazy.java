package personthecat.catlib.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Creates a sort of lazily initialized value.
 * <p>
 *   Values wrapped in this class will not exist until the first time they are used.
 *   Note that this implementation is thread safe.
 * </p>
 * @param <T> The type of value being consumed by the wrapper.
 */
@ThreadSafe
@SuppressWarnings("unused")
public class Lazy<T> implements Supplier<T> {

    /** The underlying value being wrapped by this object. */
    protected T value = null;

    /** A supplier used for producing the value when it is ready. */
    protected final Supplier<T> supplier;

    /** Whether the value has been setup. */
    protected volatile boolean set;

    /** The primary constructor with instructions for producing the value. */
    public Lazy(@NotNull final Supplier<T> supplier) {
        this.supplier = supplier;
        this.set = false;
    }

    /** To be used in the event that a value already exists. */
    public Lazy(@NotNull final T value) {
        this.value = value;
        this.supplier = () -> value;
        this.set = true;
    }

    /**
     * Factory variant of {@link #Lazy(Supplier)}.
     *
     * @param <T> The type of value being wrapped.
     * @param supplier A supplier providing this value, when the time comes.
     * @return The value, to be calculated on first use.s
     */
    public static <T> Lazy<T> of(@NotNull final Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    /**
     * Factory variant of {@link #Lazy(T)}.
     *
     * @param <T> The type of value being wrapped.
     * @param value The actual value being wrapped.
     * @return The value, consumed by the wrapper.
     */
    public static <T> Lazy<T> of(@NotNull final T value) {
        return new Lazy<>(value);
    }

    /**
     * Converts this wrapper into a resettable or non-resettable value.
     *
     * @param resettable Whether the wrapper should be resettable.
     * @return Either <code>this</code> or a {@link ResettableLazy}.
     */
    public Lazy<T> asResettable(final boolean resettable) {
        if (resettable) {
            return this.set ? new ResettableLazy<>(this.value) : new ResettableLazy<>(this.supplier);
        }
        return this;
    }

    /**
     * The primary method for retrieving the underlying value.
     *
     * @return The underlying value.
     */
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

    /** Returns the value only if it has already been computed. */
    public Optional<T> getIfComputed() {
        return Optional.ofNullable(this.value);
    }

    /** Returns whether the underlying operation has completed. */
    public boolean computed() {
        return this.set;
    }

    /** Returns an up-to-date value without resetting this value's reference. */
    public T getUpdated() {
        return this.supplier.get();
    }

    /** Returns whether the underlying data can be reloaded. */
    public boolean isResettable() {
        return false;
    }

    /** Exposes the data directly without wrapping them in {@link Optional}. */
    @Nullable
    public T expose() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.set ? this.value.toString() : "<unavailable>";
    }
}