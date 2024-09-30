package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;

import java.util.Optional;

public record OptionalCodec<A>(Codec<A> elementCodec) implements Codec<Optional<A>> {

    @Override
    public <T> DataResult<Pair<Optional<A>, T>> decode(final DynamicOps<T> ops, final T input) {
        if (input == null) {
            return DataResult.success(Pair.of(Optional.empty(), ops.empty()));
        }
        final var result = this.elementCodec.decode(ops, input).map(pair -> pair.mapFirst(Optional::of));
        if (result.isSuccess()) {
            return result;
        }
        // deferred to sad path because potentially expensive; ops.empty() != ops.empty()
        if (ops.convertTo(JavaOps.INSTANCE, input) == null) {
            return DataResult.success(Pair.of(Optional.empty(), input));
        }
        return result;
    }

    @Override
    public <T> DataResult<T> encode(final Optional<A> input, final DynamicOps<T> ops, final T prefix) {
        if (input.isEmpty()) {
            return DataResult.success(ops.empty());
        }
        return this.elementCodec.encode(input.get(), ops, prefix);
    }
}
