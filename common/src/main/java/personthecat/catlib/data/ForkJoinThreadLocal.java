package personthecat.catlib.data;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.FastThreadLocalThread;

import java.util.concurrent.ForkJoinWorkerThread;
import java.util.function.Supplier;

public abstract class ForkJoinThreadLocal<T> implements Supplier<T> {
    private static final int SIZE = Math.max(1, Math.min(255, Runtime.getRuntime().availableProcessors() - 1));
    protected final Object[] values = new Object[SIZE];
    private final FastThreadLocal<T> fallback;
    private volatile boolean warnFallback;

    private ForkJoinThreadLocal(Supplier<T> fallbackSupplier, boolean warnFallback) {
        this.fallback = new FastThreadLocal<>() {
            @Override
            protected T initialValue() {
                return fallbackSupplier.get();
            }
        };
        this.warnFallback = warnFallback;
    }

    public static <T> ForkJoinThreadLocal<T> create() {
        return create(true);
    }

    public static <T> ForkJoinThreadLocal<T> create(boolean warnFallback) {
        return eager(() -> null, warnFallback);
    }

    public static <T> ForkJoinThreadLocal<T> eager(Supplier<T> valueSupplier) {
        return eager(valueSupplier, true);
    }

    public static <T> ForkJoinThreadLocal<T> eager(Supplier<T> valueSupplier, boolean warnFallback) {
        return new Eager<>(valueSupplier, warnFallback);
    }

    public static <T> ForkJoinThreadLocal<T> lazy(Supplier<T> valueSupplier) {
        return lazy(valueSupplier, true);
    }

    public static <T> ForkJoinThreadLocal<T> lazy(Supplier<T> valueSupplier, boolean warnFallback) {
        return new Lazy<>(valueSupplier, warnFallback);
    }

    @Override
    public abstract T get();

    public final void set(T t) {
        final int idx = getThreadIndex();
        if (idx >= 0) {
            this.values[idx] = t;
        } else {
            this.setFallback(t);
        }
    }

    public final void runScoped(T value, Runnable scope) {
        this.getScoped(value, () -> {
            scope.run();
            return null;
        });
    }

    public final <R> R getScoped(T value, Supplier<R> scope) {
        final var t = this.get();
        try {
            this.set(value);
            return scope.get();
        } finally {
            this.set(t);
        }
    }

    public final void remove() {
        this.set(null);
    }

    public static int getThreadIndex() {
        if (Thread.currentThread() instanceof ForkJoinWorkerThread fjw) {
            return fjw.getPoolIndex();
        }
        return -1;
    }

    protected final T getFallback() {
        this.warnFallback();
        return this.fallback.get();
    }

    protected final void setFallback(T t) {
        this.warnFallback();
        if (t != null) {
            this.fallback.set(t);
        } else {
            this.fallback.remove();
        }
    }

    private void warnFallback() {
        if (this.warnFallback) {
            System.err.printf("Accessing thread-local data in fallback mode: %s", Mode.get());
            this.warnFallback = false;
        }
    }

    private static final class Eager<T> extends ForkJoinThreadLocal<T> {
        private Eager(Supplier<T> valueSupplier, boolean warnFallback) {
            super(valueSupplier, warnFallback);
            for (int i = 0; i < this.values.length; i++) {
                this.values[i] = valueSupplier.get();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            final int idx = getThreadIndex();
            if (idx >= 0) {
                return (T) this.values[idx];
            }
            return this.getFallback();
        }
    }

    private static final class Lazy<T> extends ForkJoinThreadLocal<T> {
        private final Supplier<T> valueSupplier;

        private Lazy(Supplier<T> valueSupplier, boolean warnFallback) {
            super(valueSupplier, warnFallback);
            this.valueSupplier = valueSupplier;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            final var i = getThreadIndex();
            var t = this.values[i];
            if (t == null) {
                t = this.values[i] = this.valueSupplier.get();
            }
            return (T) t;
        }
    }

    public enum Mode {
        FORK_JOIN,
        FAST_THREAD_LOCAL,
        STANDARD_THREAD_LOCAL;

        public static Mode get() {
            final var thread = Thread.currentThread();
            if (thread instanceof ForkJoinWorkerThread) {
                return FORK_JOIN;
            } else if (thread instanceof FastThreadLocalThread) {
                return FAST_THREAD_LOCAL;
            }
            return STANDARD_THREAD_LOCAL;
        }
    }
}