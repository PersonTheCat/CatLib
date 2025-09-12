package personthecat.catlib.serialization.codec.context;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public record CategorizedCodec<A>(Codec<A> delegate, String category) implements Codec<A> {

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        if (ops instanceof ContextualOps<T> c) {
            c.catlib$getContext().pushCategory(this.category);
        }
        try {
            return this.delegate.decode(ops, input);
        } finally {
            if (ops instanceof ContextualOps<T> c) {
                c.catlib$getContext().popCategory();
            }
        }
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        return this.delegate.encode(input, ops, prefix);
    }
}
