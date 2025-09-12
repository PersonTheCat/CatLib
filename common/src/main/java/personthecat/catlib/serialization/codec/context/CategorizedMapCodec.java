package personthecat.catlib.serialization.codec.context;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.stream.Stream;

public class CategorizedMapCodec<A> extends MapCodec<A> {
    private final MapCodec<A> delegate;
    private final String category;

    public CategorizedMapCodec(MapCodec<A> delegate, String category) {
        this.delegate = delegate;
        this.category = category;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return this.delegate.keys(ops);
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
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
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return this.delegate.encode(input, ops, prefix);
    }
}
