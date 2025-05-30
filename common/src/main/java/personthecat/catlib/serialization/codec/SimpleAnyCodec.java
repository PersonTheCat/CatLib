package personthecat.catlib.serialization.codec;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleAnyCodec<A> implements Codec<A> {
    private final Function<A, Encoder<A>> encoder;
    private final List<Decoder<? extends A>> decoders;

    @SafeVarargs
    public SimpleAnyCodec(final Decoder<? extends A> first, final Decoder<? extends A>... others) {
        this(a -> CodecUtils.toCodecUnsafe(first), first, others);
    }

    @SafeVarargs
    public SimpleAnyCodec(
            final Function<A, Encoder<? extends A>> encoder, final Decoder<? extends A> first, final Decoder<? extends A>... others) {
        this(encoder, ImmutableList.<Decoder<? extends A>>builder().add(first).add(others).build());
    }

    private SimpleAnyCodec(final Function<A, Encoder<? extends A>> encoder, final List<Decoder<? extends A>> decoders) {
        this.encoder = wrapDowncast(encoder);
        this.decoders = decoders;
    }

    public SimpleAnyCodec<A> withEncoder(final Encoder<A> encoder) {
        return this.withEncoder(a -> encoder);
    }

    public SimpleAnyCodec<A> withEncoder(final Function<A, Encoder<? extends A>> encoder) {
        return new SimpleAnyCodec<>(encoder, this.decoders);
    }

    @SuppressWarnings("unchecked")
    private static <A> Function<A, Encoder<A>> wrapDowncast(final Function<A, Encoder<? extends A>> encoder) {
        return a -> (Encoder<A>) encoder.apply(a);
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input) {
        final List<Supplier<String>> errors = new ArrayList<>();
        for (final Decoder<? extends A> decoder : this.decoders) {
            final DataResult<Pair<A, T>> result = downcast(decoder.decode(ops, input));
            if (result.result().isPresent()) {
                return result;
            }
            errors.add(result.error().orElseThrow().messageSupplier());
        }
        return DataResult.error(() -> {
            final StringBuilder message =
                new StringBuilder("Fix any; ").append(errors.getFirst().get());
            for (int i = 1; i < errors.size(); i++) {
                message.append("; ").append(errors.get(i).get());
            }
            return message.append(']').toString();
        });
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
        final StringBuilder sb = new StringBuilder("SimpleAny[").append(this.decoders.getFirst());
        for (int i = 1; i < this.decoders.size(); i++) {
            sb.append('|').append(this.decoders.get(i));
        }
        return sb.append(']').toString();
    }
}
