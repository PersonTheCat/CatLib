package personthecat.catlib.serialization.codec.capture;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ForkJoinThreadLocal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;

public final class CapturingCodec<A> implements Codec<A> {
    private static final ForkJoinThreadLocal<Deque<Captures>> FRAMES = ForkJoinThreadLocal.create(false);
    private final Codec<A> delegate;
    private final List<Captor<?>> captors;

    private CapturingCodec(Codec<A> delegate, List<Captor<?>> captors) {
        this.delegate = delegate;
        this.captors = captors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static <T> Captor<T> supply(String key, T hardDefault) {
        return supply(null, key, hardDefault);
    }

    public static <T> Captor<T> supply(@Nullable Key<?> qualifier, String key, T hardDefault) {
        return supply(Key.of(key, hardDefault).qualified(qualifier), hardDefault);
    }

    public static <T> Captor<T> supply(Key<T> key, T hardDefault) {
        return new DefaultSupplier<>(key, hardDefault);
    }

    @SafeVarargs
    public static <T> Captor<T> capture(String key, Codec<T> codec, T... implicitType) {
        return capture(null, key, codec, implicitType);
    }

    @SafeVarargs
    public static <T> Captor<T> capture(@Nullable Key<?> qualifier, String key, Codec<T> codec, T... implicitType) {
        return capture(Key.of(key, implicitType).qualified(qualifier), codec.optionalFieldOf(key));
    }

    public static <T> Captor<T> capture(Key<T> key, MapCodec<Optional<T>> codec) {
        return new DefaultGetter<>(key, codec);
    }

    @SafeVarargs
    public static <A, B extends A> Captor<A> suggestType(String name, Class<A> type, MapCodec<B> codec, B... implicitType) {
        return suggestType(Key.of(name, type), codec, implicitType);
    }

    @SafeVarargs
    public static <A, B extends A> Captor<A> suggestType(Class<A> type, MapCodec<B> codec, B... implicitType) {
        return suggestType(Key.of(Key.ANY, type), codec, implicitType);
    }

    @SafeVarargs
    public static <A, B extends A> Captor<A> suggestType(Key<A> key, MapCodec<B> codec, B... implicitType) {
        return suggestType(key, new TypeSuggestion<>(codec, Key.inferType(implicitType), key.type()));
    }

    static <A, B extends A> Captor<A> suggestType(Key<A> key, TypeSuggestion<A> suggestion) {
        return new DefaultType<>(key, suggestion);
    }

    public static <T> Receiver<T> receive(String key, T hardDefault) {
        return () -> get(Key.of(key, hardDefault), hardDefault);
    }

    @SafeVarargs
    public static <T> Receiver<T> receive(String key, T... implicitType) {
        return () -> get(Key.of(key, implicitType), null);
    }

    @SafeVarargs
    public static <A> Codec<A> receiveType(String key, Codec<A> codec, A... implicitType) {
        return receiveType("type", key, codec, implicitType);
    }

    @SafeVarargs
    public static <A> Codec<A> receiveType(String typeKey, String key, Codec<A> codec, A... implicitType) {
        return receiveType(typeKey, Key.of(key, implicitType), codec);
    }

    public static <A> Codec<A> receiveType(String key, Codec<A> codec, Class<A> type) {
        return receiveType("type", Key.of(key, type), codec);
    }

    public static <A> Codec<A> receiveType(String typeKey, Key<A> key, Codec<A> codec) {
        final var r = receiveType(key);
        return defaultType(typeKey, codec, (ops, map) -> r.getCodecRecursive(), (ops, a) -> r.getCodecRecursive())
            .filterEncoder((ops, a) -> r.getType().mapOrElse(c -> c.isInstance(a), e -> false));
    }

    public static <A> TypeReceiver<A> receiveType(Class<A> type) {
        return receiveType(Key.of(Key.ANY, type));
    }

    public static <A> TypeReceiver<A> receiveType(Key<A> key) {
        return () -> getSuggestion(key);
    }

    static <T> DataResult<T> get(Key<T> key, @Nullable T hardDefault) {
        return get(key, hardDefault, true).map(Optional::orElseThrow);
    }

    static <T> DataResult<Optional<T>> getOptional(Key<T> key, @Nullable T hardDefault) {
        return get(key, hardDefault, false);
    }

    private static <T> DataResult<Optional<T>> get(Key<T> key, @Nullable T hardDefault, boolean required) {
        final var frames = FRAMES.get();
        if (frames != null) {
            for (final var captures : frames) {
                final var result = captures.get(key);
                if (result != null) {
                    return result.map(Optional::of);
                }
            }
        }
        if (hardDefault != null) {
            return DataResult.success(Optional.of(hardDefault));
        } else if (Key.ANY.equals(key.name())) {
            return DataResult.error(() -> "No value captured for any key: " + key.type().getSimpleName());
        } else if (required) {
            return DataResult.error(() -> "No key " + key.name() + " or default value supplied");
        }
        return DataResult.success(Optional.empty());
    }

    static <A> DataResult<TypeSuggestion<A>> getSuggestion(Key<A> key) {
        final var frames = FRAMES.get();
        if (frames != null) {
            for (final var captures : frames) {
                final var result = captures.getType(key);
                if (result != null) {
                    return DataResult.success(result);
                }
            }
        }
        if (Key.ANY.equals(key.name())) {
            return DataResult.error(() -> "No default type suggested for class: " + key.type().getSimpleName());
        }
        return DataResult.error(() -> "No default type suggested for key: " + key.name());
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        return this.capture(this.applyInput(ops, input), () -> this.delegate.decode(ops, input));
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        return this.capture((captures, c) -> c.capture(captures), () -> this.delegate.encode(input, ops, prefix));
    }

    private <T> BiConsumer<Captures, Captor<?>> applyInput(DynamicOps<T> ops, T input) {
        return ops.getMap(input).mapOrElse(
            map -> (captures, c) -> c.capture(captures, ops, map),
            noMap -> (captures, c) -> c.capture(captures)
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

    public static class Builder {
        private final ImmutableList.Builder<Captor<?>> captors = ImmutableList.builder();

        public Builder capturing(Captor<?>... captors) {
            this.captors.add(captors);
            return this;
        }

        public Builder capturing(Iterable<Captor<?>> captors) {
            this.captors.addAll(captors);
            return this;
        }

        public <A> MapCodec<A> build(MapCodec<A> codec) {
            return MapCodec.assumeMapUnsafe(this.build(codec.codec()));
        }

        public <A> Codec<A> build(Codec<A> codec) {
            return new CapturingCodec<>(codec, this.captors.build());
        }
    }
}