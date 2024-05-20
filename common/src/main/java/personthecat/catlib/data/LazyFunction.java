package personthecat.catlib.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.annotation.concurrent.ThreadSafe;

import java.util.Optional;
import java.util.function.Function;

/**
 * Creates a sort of lazily initialized value.
 * <p>
 *   Values wrapped in this class will not exist until the first time they are used.
 *   Note that this implementation thread safe.
 * </p>
 * @param <T> The type of value being consumed by the wrapper.
 * @param <R> The type of value being returned by the wrapper.
 */
@ThreadSafe
public class LazyFunction<T, R> {

    /** The underlying value being wrapped by this object. */
    protected R value = null;

    /** A function used for creating the value when it is ready. */
    protected final Function<T, R> function;

    /** Whether the value has been setup. */
    protected volatile boolean set;

    /** The primary constructor with instructions for producing the value. */
    public LazyFunction(@NotNull final Function<T, R> function) {
        this.function = function;
        this.set = false;
    }

    /**
     * Factory variant of {@link #LazyFunction(Function)}.
     *
     * @param <T> The type of value being accepted.
     * @param <R> The type of data being wrapped.
     * @param function A function used for creating this value, when the time comes.
     * @return The value, to be calculated on first use.s
     */
    public static <T, R> LazyFunction<T, R> of(final Function<T, R> function) {
        return new LazyFunction<>(function);
    }

    /**
     * The primary method for retrieving the underlying value.
     *
     * @param t Any data required by the function.
     * @return The underlying value.
     */
    @NotNull
    public R apply(final T t) {
        if (!this.set) {
            synchronized(this) {
                this.value = this.function.apply(t);
                this.set = true;
            }
        }
        return this.value;
    }

    /**
     * Converts this into a regular {@link Lazy} instance. This may be useful
     * when the required data have become available, but computing the result
     * is currently unnecessary.
     *
     * @param t Any data required by the function.
     * @return A new {@link Lazy} wrapper requiring no inputs.
     */
    public Lazy<R> asLazy(final T t) {
        return Lazy.of(() -> this.apply(t));
    }

    /**
     * Returns the value only if it has already been computed.
     *
     * @return The underlying value, or else {@link Optional#empty}.
     */
    public Optional<R> getIfComputed() {
        return Optional.ofNullable(this.value);
    }

    /**
     * Returns whether the underlying operation has completed.
     *
     * @return <code>true</code>, if the value has been computed.
     */
    public boolean computed() {
        return this.set;
    }

    /**
     * Returns an up-to-date value without resetting this value's reference.
     *
     * @param t The generic input to this function.
     * @return The result of this function, non-lazily.
     */
    public R applyUpdated(final T t) {
        return this.function.apply(t);
    }

    /**
     * Exposes the data directly without wrapping them in {@link Optional}.
     *
     * @return The raw value, or else <code>null</code>.
     */
    @Nullable
    public R expose() {
        return this.value;
    }

    public String toString() {
        return this.set ? this.value.toString() : "<unavailable>";
    }
}
