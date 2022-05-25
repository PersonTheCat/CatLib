package personthecat.catlib.serialization.codec;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unused")
public class SimpleAnyCodec<A> implements Codec<A> {
    private final Function<A, Encoder<A>> encoder;
    private final List<Codec<? extends A>> codecs;

    @SafeVarargs
    public SimpleAnyCodec(final Codec<? extends A> first, final Codec<? extends A>... others) {
        this(a -> first, first, others);
    }

    @SafeVarargs
    public SimpleAnyCodec(
            final Function<A, Encoder<? extends A>> encoder, final Codec<? extends A> first, final Codec<? extends A>... others) {
        this(encoder, ImmutableList.<Codec<? extends A>>builder().add(first).add(others).build());
    }

    private SimpleAnyCodec(final Function<A, Encoder<? extends A>> encoder, final List<Codec<? extends A>> codecs) {
        this.encoder = wrapDowncast(encoder);
        this.codecs = codecs;
    }

    public SimpleAnyCodec<A> withEncoder(final Encoder<A> encoder) {
        return this.withEncoder(a -> encoder);
    }

    public SimpleAnyCodec<A> withEncoder(final Function<A, Encoder<? extends A>> encoder) {
        return new SimpleAnyCodec<>(encoder, this.codecs);
    }

    @SuppressWarnings("unchecked")
    private static <A> Function<A, Encoder<A>> wrapDowncast(final Function<A, Encoder<? extends A>> encoder) {
        return a -> (Encoder<A>) encoder.apply(a);
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input) {
        final List<String> errors = new ArrayList<>();
        for (final Codec<? extends A> codec : this.codecs) {
            final DataResult<Pair<A, T>> result = downcast(codec.decode(ops, input));
            if (result.result().isPresent()) {
                return result;
            }
            result.error().ifPresent(e -> errors.add(e.message()));
        }
        final StringBuilder message =
            new StringBuilder("Fix any: [\"").append('"').append(errors.get(0)).append(",\"");
        for (int i = 1; i < errors.size(); i++) {
            message.append('"').append(errors.get(i)).append('"');
        }
        return DataResult.error(message.append(']').toString());
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
        final StringBuilder sb = new StringBuilder("SimpleAny[").append(this.codecs.get(0));
        for (int i = 1; i < this.codecs.size(); i++) {
            sb.append('|').append(this.codecs.get(i));
        }
        return sb.append(']').toString();
    }
}
