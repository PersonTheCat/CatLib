package personthecat.catlib.serialization;

import com.mojang.serialization.*;

import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class DefaultedMapCodec<A> extends MapCodec<A> {

    private final Codec<A> codec;
    private final MapCodec<A> map;
    private final String key;
    private final Supplier<A> def;

    public DefaultedMapCodec(final Codec<A> codec, final String key, final Supplier<A> def) {
        this.codec = codec;
        this.map = codec.fieldOf(key);
        this.key = key;
        this.def = def;
    }

    public Codec<A> getType() {
        return this.codec;
    }

    public MapCodec<A> wrapped() {
        return this.map;
    }

    public String key() {
        return this.key;
    }

    public A defaultValue() {
        return this.def.get();
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops) {
        return this.map.keys(ops);
    }

    @Override
    public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        if (input.get(this.key) == null) {
            return DataResult.success(this.defaultValue());
        }
        return this.map.decode(ops, input);
    }

    @Override
    public <T> RecordBuilder<T> encode(final A input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
        return this.map.encode(input, ops, prefix);
    }

    @Override
    public String toString() {
        return "DefaultedMapCodec[" + this.key + " -> " + this.codec + "]";
    }
}
