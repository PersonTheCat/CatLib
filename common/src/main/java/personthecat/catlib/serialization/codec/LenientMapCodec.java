package personthecat.catlib.serialization.codec;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.Optional;
import java.util.stream.Stream;

public class LenientMapCodec<A> extends MapCodec<Optional<A>> {
    private final MapCodec<A> codec;

    public LenientMapCodec(MapCodec<A> codec) {
        this.codec = codec;
    }

    public static <A> LenientMapCodec<A> of(MapCodec<A> codec) {
        return new LenientMapCodec<>(codec);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return this.codec.keys(ops);
    }

    @Override
    public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
        return this.codec.decode(ops, input).mapOrElse(
            success -> DataResult.success(Optional.of(success)),
            error -> DataResult.success(Optional.empty())
        );
    }

    @Override
    public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return input.map(a -> this.codec.encode(a, ops, prefix)).orElse(prefix);
    }
}
