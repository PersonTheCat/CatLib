package personthecat.catlib.serialization.codec.context;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;

public record TrackingDecoder<A>(Decoder<A> delegate) implements Decoder<A> {
    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        return this.delegate.decode(ops, input).ifError(e -> {
            if (ops instanceof ContextualOps<T> c) {
                c.catlib$getContext().reportError(e);
                c.catlib$getContext().recordInput(input);
            }
        });
    }
}
