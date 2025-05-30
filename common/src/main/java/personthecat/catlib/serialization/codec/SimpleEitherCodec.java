package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;

import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleEitherCodec<A> implements Codec<A> {
    private final Decoder<? extends A> first;
    private final Decoder<? extends A> second;
    private final Function<A, Encoder<A>> encoder;

    public SimpleEitherCodec(final Decoder<? extends A> first, final Decoder<? extends A> second) {
        this(first, second, wrapDowncast(a -> CodecUtils.toCodecUnsafe(first)));
    }

    public SimpleEitherCodec(
            final Decoder<? extends A> first, final Decoder<? extends A> second, final Function<A, Encoder<A>> encoder) {
        this.first = first;
        this.second = second;
        this.encoder = encoder;
    }

    public SimpleEitherCodec<A> withEncoder(final Encoder<? extends A> encoder) {
        return this.withEncoder(a -> encoder);
    }

    public SimpleEitherCodec<A> withEncoder(final Function<A, Encoder<? extends A>> encoder) {
        return new SimpleEitherCodec<>(this.first, this.second, wrapDowncast(encoder));
    }

    @SuppressWarnings("unchecked")
    private static <A> Function<A, Encoder<A>> wrapDowncast(final Function<A, Encoder<? extends A>> encoder) {
        return a -> (Encoder<A>) encoder.apply(a);
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input) {
        final DataResult<Pair<A, T>> r1 = downcast(this.first.decode(ops, input));
        if (r1.result().isPresent()) {
            return r1;
        }
        final DataResult<Pair<A, T>> r2 = downcast(this.second.decode(ops, input));
        if (r2.result().isPresent()) {
            return r2;
        }
        final Supplier<String> m1 = r1.error().orElseThrow().messageSupplier();
        final Supplier<String> m2 = r2.error().orElseThrow().messageSupplier();
        return DataResult.error(() -> "Fix either; " + m1.get() + "; " + m2.get());
    }

    @SuppressWarnings("unchecked")
    private static <A, B extends A, T> DataResult<Pair<A, T>> downcast(final DataResult<Pair<B, T>> pair) {
        return (DataResult<Pair<A, T>>) (Object) pair;
    }

    @Override
    public <T> DataResult<T> encode(final A input, final DynamicOps<T> ops, final T prefix) {
        return this.encoder.apply(input).encode(input, ops, prefix);
    }

    @Override
    public String toString() {
        return "SimpleEither[" + this.first + " | " + this.second + "]";
    }
}
