package personthecat.catlib.serialization.codec;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class FilteredMapCodec<A> extends MapCodec<A> {
    private final MapCodec<A> delegate;
    private final Predicate<A> filter;

    public FilteredMapCodec(MapCodec<A> delegate, Predicate<A> filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return this.delegate.keys(ops);
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        return this.delegate.decode(ops, input);
    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (this.filter.test(input)) {
            return this.delegate.encode(input, ops, prefix);
        }
        return prefix;
    }

    @Override
    public String toString() {
        return "Filtered[" + this.delegate + "]";
    }
}
