package personthecat.catlib.serialization.codec;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class StrictOptionalCodec<A> extends MapCodec<Optional<A>> {
    private final Codec<A> codec;
    private final MapCodec<A> map;
    private final String key;

    public StrictOptionalCodec(final Codec<A> codec, final String key) {
        this.codec = codec;
        this.map = codec.fieldOf(key);
        this.key = key;
    }

    public Codec<A> type() {
        return this.codec;
    }

    public MapCodec<A> wrapped() {
        return this.map;
    }

    public String key() {
        return this.key;
    }

    public <O> RecordCodecBuilder<O, Optional<A>> wrapGetter(final Function<O, A> getter) {
        return this.forGetter(o -> Optional.ofNullable(getter.apply(o)));
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops) {
        return this.map.keys(ops);
    }

    @Override
    public <T> DataResult<Optional<A>> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        if (input.get(this.key) == null) {
            return DataResult.success(Optional.empty());
        }
        return this.map.decode(ops, input).map(Optional::ofNullable);
    }

    @Override
    public <T> RecordBuilder<T> encode(final Optional<A> input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
        if (input.isPresent()) {
            return this.map.encode(input.get(), ops, prefix);
        }
        return prefix;
    }

    @Override
    public String toString() {
        return "StrictOptionalCodec[" + this.key + " -> " + this.codec + "]";
    }
}