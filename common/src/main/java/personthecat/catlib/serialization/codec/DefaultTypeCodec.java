package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class DefaultTypeCodec<A> implements Codec<A> {
    private final String typeKey;
    private final Codec<A> dispatcher;
    private final BiFunction<DynamicOps<?>, MapLike<?>, DataResult<? extends MapDecoder<? extends A>>> defaultDecoder;
    private final BiFunction<DynamicOps<?>, ? super A, DataResult<? extends MapEncoder<? extends A>>> defaultEncoder;

    public DefaultTypeCodec(
            final String typeKey,
            final Codec<A> dispatcher,
            final BiFunction<DynamicOps<?>, MapLike<?>, DataResult<? extends MapDecoder<? extends A>>> defaultDecoder,
            final BiFunction<DynamicOps<?>, ? super A, DataResult<? extends MapEncoder<? extends A>>> defaultEncoder) {
        this.typeKey = typeKey;
        this.dispatcher = dispatcher;
        this.defaultDecoder = defaultDecoder;
        this.defaultEncoder = defaultEncoder;
    }

    public DefaultTypeCodec<A> filterEncoder(final BiPredicate<DynamicOps<?>, ? super A> filter) {
        final var original = this.defaultEncoder;
        return new DefaultTypeCodec<>(this.typeKey, this.dispatcher, this.defaultDecoder, (ops, a) ->
            filter.test(ops, a) ? original.apply(ops, a) : DataResult.error(() -> "Could not infer default encoder"));
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input) {
        if (ops.compressMaps()) {
            return this.dispatcher.decode(ops, input);
        }
        return ops.getMap(input).mapOrElse(
            map -> this.decodeMap(ops, map, input).map(a -> Pair.of(a, input)),
            noMap -> this.dispatcher.decode(ops, input)
        );
    }

    private <T> DataResult<? extends A> decodeMap(final DynamicOps<T> ops, final MapLike<T> map, final T input) {
        if (map.get(this.typeKey) != null) {
            return CodecUtils.asMapCodec(this.dispatcher).decode(ops, map);
        }
        final var defaultResult = this.defaultDecoder.apply(ops, map).flatMap(codec -> codec.decode(ops, map));
        if (defaultResult.resultOrPartial().isPresent()) {
            return defaultResult;
        }
        final var dispatcherResult = this.dispatcher.decode(ops, input).map(Pair::getFirst);
        if (dispatcherResult.resultOrPartial().isPresent()) {
            return dispatcherResult;
        }
        final var m1 = defaultResult.error().orElseThrow().messageSupplier();
        final var m2 = dispatcherResult.error().orElseThrow().messageSupplier();
        return DataResult.error(() ->
            "Explicit type missing or error on default type; " + m1.get() + "; " + m2.get());
    }

    @Override
    public <T> DataResult<T> encode(final A input, final DynamicOps<T> ops, final T prefix) {
        if (ops.compressMaps()) {
            return this.dispatcher.encode(input, ops, prefix);
        }
        return this.defaultEncoder.apply(ops, input).mapOrElse(
            encoder -> this.encodeMap(upcast(encoder), input, ops, prefix),
            noEncoder -> this.dispatcher.encode(input, ops, prefix)
        );
    }

    private <T> DataResult<T> encodeMap(
            final MapEncoder<A> encoder, final A input, final DynamicOps<T> ops, final T prefix) {
        final var defaultResult = encoder.encode(input, ops, encoder.compressedBuilder(ops)).build(prefix);
        if (defaultResult.isSuccess()) {
            return defaultResult;
        }
        final var dispatcherResult = this.dispatcher.encode(input, ops, prefix);
        if (dispatcherResult.isSuccess()) {
            return dispatcherResult;
        }
        final var m1 = defaultResult.error().orElseThrow().messageSupplier();
        final var m2 = dispatcherResult.error().orElseThrow().messageSupplier();
        return DataResult.error(() ->
            "Could not encode via dispatcher or default type; " + m1.get() + "; " + m2.get());
    }

    @SuppressWarnings("unchecked")
    private static <A> MapEncoder<A> upcast(MapEncoder<? extends A> encoder) {
        return (MapEncoder<A>) encoder;
    }

    @Override
    public String toString() {
        return "Defaulted[" + this.dispatcher + "]";
    }
}
