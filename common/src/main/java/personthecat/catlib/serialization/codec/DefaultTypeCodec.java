package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class DefaultTypeCodec<A> implements Codec<A> {
    private final Codec<A> dispatcher;
    private final Codec<A> defaultType;
    private final BiPredicate<A, DynamicOps<?>> isDefaultType;

    public DefaultTypeCodec(
            final Codec<A> dispatcher,
            final Codec<A> defaultType,
            final BiPredicate<A, DynamicOps<?>> isDefaultType) {
        this.dispatcher = dispatcher;
        this.defaultType = defaultType;
        this.isDefaultType = isDefaultType;
    }

    @Override
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input) {
        if (ops.compressMaps() || this.hasExplicitType(ops, input)) {
            return this.dispatcher.decode(ops, input);
        }
        final var defaultResult = this.defaultType.decode(ops, input);
        if (defaultResult.resultOrPartial().isPresent()) {
            return defaultResult;
        }
        final var dispatcherResult = this.dispatcher.decode(ops, input);
        if (dispatcherResult.resultOrPartial().isPresent()) {
            return dispatcherResult;
        }
        final Supplier<String> m1 = defaultResult.error().get().messageSupplier();
        final Supplier<String> m2 = dispatcherResult.error().get().messageSupplier();
        return DataResult.error(() ->
            "Either provide an explicit type or fix: [" + m1.get() + "," + m2.get() + "]");
    }

    private <T> boolean hasExplicitType(final DynamicOps<T> ops, final T input) {
        return ops.getMap(input).mapOrElse(map -> map.get("type") != null, e -> false);
    }

    @Override
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public <T> DataResult<T> encode(final A input, final DynamicOps<T> ops, final T prefix) {
        if (ops.compressMaps() || !this.isDefaultType.test(input, ops)) {
            return this.dispatcher.encode(input, ops, prefix);
        }
        final var defaultResult = this.defaultType.encode(input, ops, prefix);
        if (defaultResult.isSuccess()) {
            return defaultResult;
        }
        return this.dispatcher.encode(input, ops, prefix)
            .mapError(e -> DataResult.appendMessages(e, defaultResult.error().get().message()));
    }

    @Override
    public String toString() {
        return this.dispatcher + " default " + this.defaultType;
    }
}
