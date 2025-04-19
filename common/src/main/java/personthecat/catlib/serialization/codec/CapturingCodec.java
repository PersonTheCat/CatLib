package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ForkJoinThreadLocal;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CapturingCodec<A> extends MapCodec<A> {
    private static final ForkJoinThreadLocal<Deque<MapFrame<?>>> FRAMES = ForkJoinThreadLocal.create(false);
    protected final MapCodec<A> delegate;

    private CapturingCodec(MapCodec<A> delegate) {
        this.delegate = delegate;
    }

    public static <A> MapCodec<A> captor(MapCodec<A> delegate) {
        return new Captor<>(delegate, null);
    }

    public static <A> MapCodec<A> captor(MapCodec<A> delegate, MapCodec<?>... captureSet) {
        return captor(delegate, Stream.of(captureSet).flatMap(c -> c.keys(JavaOps.INSTANCE)).collect(Collectors.toSet()));
    }

    public static <A> MapCodec<A> captor(MapCodec<A> delegate, Collection<?> fields) {
        return new Captor<>(delegate, fields::contains);
    }

    public static <A> MapCodec<A> receiver(MapCodec<A> delegate) {
        return new Receiver<>(delegate);
    }

    private static <T, R> R capture(DynamicOps<T> ops, MapLike<T> map, Function<MapStack<T>, R> fn) {
        var frames = FRAMES.get();
        if (frames == null) {
            FRAMES.set(frames = new ArrayDeque<>());
        }
        frames.add(new MapFrame<>(ops, map));
        try {
            return fn.apply(new MapStack<>(frames.reversed(), ops));
        } finally {
            frames.pop();
            if (frames.isEmpty()) {
                FRAMES.remove();
            }
        }
    }

    private static class Captor<A> extends CapturingCodec<A> {
        protected final @Nullable Predicate<String> filter;

        private Captor(MapCodec<A> delegate, @Nullable Predicate<String> filter) {
            super(delegate);
            this.filter = filter;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return this.delegate.keys(ops);
        }

        @Override
        public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
            return capture(ops, this.filterMap(ops, input), stack -> this.delegate.decode(ops, input));
        }

        @Override
        public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return this.delegate.encode(input, ops, prefix);
        }

        protected <T> MapLike<T> filterMap(DynamicOps<T> ops, MapLike<T> map) {
            return this.filter != null ? new FilteredMap<>(map, ops, this.filter) : map;
        }
    }

    private static class Receiver<A> extends CapturingCodec<A> {

        private Receiver(MapCodec<A> delegate) {
            super(delegate);
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return this.delegate.keys(ops);
        }

        @Override
        public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
            return capture(ops, input, stack -> this.delegate.decode(ops, stack));
        }

        @Override
        public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return this.delegate.encode(input, ops, prefix);
        }
    }

    private record FilteredMap<T>(
            MapLike<T> map, DynamicOps<T> ops, Predicate<String> predicate) implements MapLike<T> {

        @Override
        public T get(T key) {
            return this.testKey(key) ? this.map.get(key) : null;
        }

        @Override
        public T get(String key) {
            return this.predicate.test(key) ? this.map.get(key) : null;
        }

        @Override
        public Stream<Pair<T, T>> entries() {
            return this.map.entries().filter(p -> this.testKey(p.getFirst()));
        }

        private boolean testKey(T t) {
            return this.ops.getStringValue(t).mapOrElse(this.predicate::test, e -> false);
        }
    }

    private record MapStack<T>(Deque<MapFrame<?>> frames, DynamicOps<T> ops) implements MapLike<T> {

        @Override
        public T get(T key) {
            for (final var frame : this.frames) {
                final var t = frame.get(this.ops, key);
                if (t != null) {
                    return t;
                }
            }
            return null;
        }

        @Override
        public T get(String key) {
            for (final var frame : this.frames) {
                final var t = frame.get(this.ops, key);
                if (t != null) {
                    return t;
                }
            }
            return null;
        }

        @Override
        public Stream<Pair<T, T>> entries() {
            return this.frames.stream()
                .flatMap(frame -> frame.entries(this.ops))
                .filter(distinctBy(Pair::getFirst));
        }

        public static <T> Predicate<T> distinctBy(Function<? super T, ?> keyExtractor) {
            final var seen = ConcurrentHashMap.newKeySet();
            return t -> !seen.add(keyExtractor.apply(t));
        }
    }

    private record MapFrame<T>(DynamicOps<T> ops, MapLike<T> map) {

        private <R> R get(DynamicOps<R> type, R key) {
            return convert(this.ops, type, this.map.get(convert(type, this.ops, key)));
        }

        private <R> R get(DynamicOps<R> type, String key) {
            return convert(this.ops, type, this.map.get(key));
        }

        private <R> Stream<Pair<R, R>> entries(DynamicOps<R> type) {
            return this.map.entries().map(p ->
                Pair.of(convert(this.ops, type, p.getFirst()), convert(this.ops, type, p.getSecond())));
        }

        @SuppressWarnings("unchecked")
        private static <T, R> R convert(DynamicOps<T> from, DynamicOps<R> to, T t) {
            if (t == null) {
                return null;
            } else if (from == to) {
                return (R) t;
            }
            return from.convertTo(to, t);
        }
    }
}