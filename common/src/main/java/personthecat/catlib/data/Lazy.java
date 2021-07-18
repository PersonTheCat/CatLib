package personthecat.catlib.data;

import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Creates a sort of lazily initialized value.
 * <p>
 *   Values wrapped in this class will not exist until the first time they are used.
 *   Note that this implementation is <b>not thread safe</b>.
 * </p>
 * @param <T> The type of value being consumed by the wrapper.
 */
@NotThreadSafe
@SuppressWarnings("unused")
public class Lazy<T> implements Supplier<T> {

    /** The underlying value being wrapped by this object. */
    protected T value = null;

    /** A supplier used for producing the value when it is ready. */
    protected Supplier<T> supplier;

    /** The primary constructor with instructions for producing the value. */
    Lazy(@NotNull final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /** To be used in the event that a value already exists. */
    Lazy(@NotNull final T value) {
        this.value = value;
        this.supplier = null;
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
     * The primary method for retrieving the underlying value.
     *
     * @return The underlying value.
     */
    @NotNull
    @Override
    public T get() {
        if (this.supplier != null) {
            this.value = Objects.requireNonNull(this.supplier.get(), "Lazy value");
            this.supplier = null;
        }
        return this.value;
    }

    /** Returns the value only if it has already been computed. */
    public Optional<T> getIfComputed() {
        return Optional.ofNullable(this.value);
    }

    /** Returns whether the underlying operation has completed. */
    public boolean computed() {
        return this.value != null;
    }
}