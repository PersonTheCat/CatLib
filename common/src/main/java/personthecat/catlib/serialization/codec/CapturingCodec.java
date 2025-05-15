package personthecat.catlib.serialization.codec;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ForkJoinThreadLocal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CapturingCodec<A> extends MapCodec<A> {
    private static final ForkJoinThreadLocal<Deque<Captures>> FRAMES = ForkJoinThreadLocal.create(false);
    protected final List<Captor<?>> captors;
    protected final MapCodec<A> delegate;

    private CapturingCodec(MapCodec<A> delegate, List<Captor<?>> captors) {
        this.captors = captors;
        this.delegate = delegate;
    }

    public static <A> CapturingCodec<A> of(MapCodec<A> delegate) {
        return new CapturingCodec<>(delegate, List.of());
    }

    public static <T> Captor<T> supply(String key, T hardDefault) {
        return new DefaultSupplier<>(Key.of(key, hardDefault), hardDefault);
    }

    @SafeVarargs
    public static <T> Captor<T> capture(String key, Codec<T> codec, T... implicitType) {
        return capture(key, codec.fieldOf(key), implicitType);
    }

    @SafeVarargs
    public static <T> Captor<T> capture(String key, MapCodec<T> codec, T... implicitType) {
        return new DefaultGetter<>(Key.of(key, implicitType), codec);
    }

    public static <T> Receiver<T> receive(String key, T hardDefault) {
        return () -> get(Key.of(key, hardDefault), hardDefault);
    }

    @SafeVarargs
    public static <T> Receiver<T> receive(String key, T... implicitType) {
        return () -> get(Key.of(key, implicitType), null);
    }

    protected static <T> DataResult<T> get(Key<T> key, @Nullable T hardDefault) {
        final var frames = FRAMES.get();
        if (frames != null) {
            for (final var captures : frames) {
                final var result = captures.get(key);
                if (result != null) {
                    return result;
                }
            }
        }
        if (hardDefault != null) {
            return DataResult.success(hardDefault);
        }
        return DataResult.error(() -> "No key " + key.key);
    }

    public CapturingCodec<A> capturing(Captor<?>... captors) {
        return new CapturingCodec<>(this.delegate, append(this.captors, captors));
    }

    @SafeVarargs
    private static <T> List<T> append(List<T> list, T... entries) {
        return ImmutableList.<T>builder().addAll(list).add(entries).build();
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.concat(
            this.captors.stream().map(c -> ops.createString(c.key().key)),
            this.delegate.keys(ops)
        );
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        return this.capture(
            (captures, captor) -> captor.capture(captures, ops, input),
            () -> this.delegate.decode(ops, input)
        );
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return this.capture(
            (captures, captor) -> captor.capture(captures),
            () -> this.delegate.encode(input, ops, prefix)
        );
    }

    private <R> R capture(BiConsumer<Captures, Captor<?>> applicative, Supplier<R> f) {
        var frames = FRAMES.get();
        if (frames == null) {
            FRAMES.set(frames = new ArrayDeque<>());
        }
        final var captures = new Captures();
        for (final var captor : this.captors) {
            applicative.accept(captures, captor);
        }
        frames.add(captures);
        try {
            return f.get();
        } finally {
            frames.removeLast();
            if (frames.isEmpty() ) {
                FRAMES.remove();
            }
        }
    }

    protected record DefaultSupplier<T>(Key<T> key, T hardDefault) implements Captor<T> {

        @Override
        public void capture(Captures captures) {
            captures.put(this.key, () -> DataResult.success(this.hardDefault));
        }
    }

    protected record DefaultGetter<T>(Key<T> key, MapCodec<T> codec) implements Captor<T> {

        @Override
        public <O> void capture(Captures captures, DynamicOps<O> ops, MapLike<O> input) {
            captures.put(this.key, () -> this.codec.decode(ops, input));
        }
    }

    public interface Captor<T> {
        Key<T> key();
        default void capture(Captures captures) {}
        default <O> void capture(Captures captures, DynamicOps<O> ops, MapLike<O> input) {
            this.capture(captures);
        }
    }

    public record Captures(Map<Key<?>, Supplier<DataResult<?>>> map) {
        public Captures() {
            this(new HashMap<>());
        }

        @SuppressWarnings("unchecked")
        public <T> void put(Key<T> key, Supplier<DataResult<T>> supplier) {
            this.map.put(key, (Supplier<DataResult<?>>) (Object) supplier);
        }

        @SuppressWarnings("unchecked")
        public <T> @Nullable DataResult<T> get(Key<T> key) {
            return (DataResult<T>) this.map.getOrDefault(key, () -> null).get();
        }
    }

    public record Key<T>(String key, Class<T> type) {
        @SuppressWarnings("unchecked")
        public static <T> Key<T> of(String key, T t) {
            return of(key, (Class<T>) t.getClass());
        }

        @SuppressWarnings("unchecked")
        public static <T> Key<T> of(String key, T... implicitType) {
            return of(key, (Class<T>) implicitType.getClass().getComponentType());
        }

        public static <T> Key<T> of(String key, Class<T> type) {
            return new Key<>(key, type);
        }
    }

    @FunctionalInterface
    public interface Receiver<T> extends Supplier<DataResult<T>> {}
}