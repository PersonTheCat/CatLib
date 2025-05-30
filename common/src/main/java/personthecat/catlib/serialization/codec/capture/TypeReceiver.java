package personthecat.catlib.serialization.codec.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import static personthecat.catlib.serialization.codec.CodecUtils.defaultType;
import static personthecat.catlib.serialization.codec.capture.CapturingCodec.suggestType;

@FunctionalInterface
public interface TypeReceiver<A> extends Supplier<DataResult<TypeSuggestion<A>>> {

    default DataResult<Class<? extends A>> getType() {
        return this.get().map(TypeSuggestion::type);
    }

    default DataResult<MapCodec<? extends A>> getCodec() {
        return this.get().map(TypeSuggestion::codec);
    }

    default DataResult<MapCodec<? extends A>> getCodecRecursive() {
        return this.get().map(TypeReceiver::recapture);
    }

    default Codec<A> wrap(Codec<A> codec) {
        return this.wrap("type", codec);
    }

    default Codec<A> wrap(String typeKey, Codec<A> codec) {
        return defaultType(typeKey, codec, this.decoder(), this.encoder());
    }

    default BiFunction<DynamicOps<?>, MapLike<?>, DataResult<? extends MapDecoder<? extends A>>> decoder() {
        return (ops, map) -> this.getCodec();
    }

    default BiFunction<DynamicOps<?>, ? super A, DataResult<? extends MapEncoder<? extends A>>> encoder() {
        return (ops, a) -> this.getCodec();
    }

    private static <A> MapCodec<? extends A> recapture(TypeSuggestion<A> s) {
        return CapturingCodec.builder().capturing(suggestType(Key.of(Key.ANY, s.parentType()), s)).build(s.codec());
    }
}
