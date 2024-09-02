package personthecat.catlib.serialization.codec;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class DefaultTypeCodec<A> extends MapCodec<A> {
    private final MapCodec<A> dispatcher;
    private final MapCodec<A> defaultType;
    private final BiPredicate<A, DynamicOps<?>> isDefaultType;

    public DefaultTypeCodec(
            final MapCodec<A> dispatcher,
            final MapCodec<A> defaultType,
            final BiPredicate<A, DynamicOps<?>> isDefaultType) {
        this.dispatcher = dispatcher;
        this.defaultType = defaultType;
        this.isDefaultType = isDefaultType;
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops) {
        return this.dispatcher.keys(ops);
    }

    @Override
    public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        return ops.compressMaps() || input.get("type") != null
            ? this.dispatcher.decode(ops, input)
            : this.defaultType.decode(ops, input);
    }

    @Override
    public <T> RecordBuilder<T> encode(final A input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
        return ops.compressMaps() || !this.isDefaultType.test(input, ops)
            ? this.dispatcher.encode(input, ops, prefix)
            : this.defaultType.encode(input, ops, prefix);
    }

    @Override
    public String toString() {
        return this.dispatcher + " default " + this.defaultType;
    }
}
