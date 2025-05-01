package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.junit.jupiter.api.Test;
import xjs.data.Json;
import xjs.data.JsonValue;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static personthecat.catlib.serialization.codec.CodecUtils.ifMap;
import static personthecat.catlib.serialization.codec.CodecUtils.neverMapCodec;

public class IfMapCodecTest {

    @Test
    public void decode_usesMapCodec_whenMap() {
        final var subject = ifMap(Data.CODEC, Data.OPTIONAL_PLUS);
        final var result = Data.parse(subject, "value: 2, plus: 2");
        assertTrue(result.isSuccess());
        assertEquals(4, result.getOrThrow().value);
    }

    @Test
    public void decode_usesOriginalCodec_whenNotMap() {
        final var subject = ifMap(Data.CODEC, neverMapCodec());
        final var result = Data.parse(subject, "1");
        assertTrue(result.isSuccess());
        assertEquals(1, result.getOrThrow().value);
    }

    @Test
    public void encode_usesMapCodec_byFunctionResult() {
        final var subject = ifMap(Data.CODEC, Data.ENCODE_HALF_PLUS, (d, o) -> true);
        final var result = Data.encode(subject, new Data(4));
        assertTrue(result.isSuccess());
        assertEquals(Json.object().add("value", 2).add("plus", 2), result.getOrThrow());
    }

    @Test
    public void encode_usesOriginalCodec_byDefault() {
        final var subject = ifMap(Data.CODEC, neverMapCodec());
        final var result = Data.encode(subject, new Data(5));
        assertTrue(result.isSuccess());
        // original can be a MapCodecCodec, thus a map is still valid output
        assertEquals(Json.object().add("value", 5), result.getOrThrow());
    }

    private record Data(int value) {
        private static final Codec<Data> DIRECT_CODEC =
            Codec.INT.fieldOf("value").xmap(Data::new, Data::value).codec();
        private static final Codec<Data> CODEC = Codec.either(Codec.INT, DIRECT_CODEC).xmap(
            e -> e.map(Data::new, Function.identity()),
            Either::right
        );
        // In a real scenario, we would be adding this extra field to an existing codec
        // but only in the case where we don't want to directly modify the original
        private static final MapCodec<Data> OPTIONAL_PLUS = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("value").forGetter(Data::value),
            Codec.INT.optionalFieldOf("plus", 0).forGetter(d -> 0)
        ).apply(i, Data::withPlus));
        private static final MapCodec<Data> ENCODE_HALF_PLUS = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("value").forGetter(d -> (int) Math.floor((double) d.value / 2.0)),
            Codec.INT.fieldOf("plus").forGetter(d -> (int) Math.ceil((double) d.value / 2.0))
        ).apply(i, Data::withPlus));

        private static Data withPlus(int value, int plus) {
            return new Data(value + plus);
        }

        private static DataResult<Data> parse(Codec<Data> subject, String json) {
            return subject.parse(XjsOps.INSTANCE, Json.parse(json));
        }

        private static DataResult<JsonValue> encode(Codec<Data> subject, Data data) {
            return subject.encodeStart(XjsOps.INSTANCE, data);
        }
    }
}
