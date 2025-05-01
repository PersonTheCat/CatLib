package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;

import java.util.function.BiPredicate;

public record IfMapCodec<A>(Codec<A> codec, MapCodec<A> map, BiPredicate<A, DynamicOps<?>> isMap) implements Codec<A> {

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        if (ops.compressMaps()) {
            return this.map.compressedDecode(ops, input).map(r -> Pair.of(r, input));
        }
        return ops.getMap(input).mapOrElse(
            map -> this.map.decode(ops, map).map(r -> Pair.of(r, input)),
            err -> this.codec.decode(ops, input)
        );
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        if (ops.compressMaps() || this.isMap.test(input, ops)) {
            return this.map.encode(input, ops, this.map.compressedBuilder(ops)).build(prefix);
        }
        return this.codec.encode(input, ops, prefix);
    }

    @Override
    public String toString() {
        return "IfMap[" + this.map + " else " + this.codec + "]";
    }
}
