package personthecat.catlib.serialization;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import org.hjson.JsonArray;
import org.hjson.JsonValue;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AutoFlatListCodecTest {

    private static final Codec<List<Integer>> CODEC = new AutoFlatListCodec<>(Codec.INT);

    @Test
    public final void encode_writesNormalList() {
        assertEquals(new JsonArray().add(1).add(2).add(3), encode(Arrays.asList(1, 2, 3)));
    }

    @Test
    public final void encode_writesSingletonAsValue() {
        assertEquals(JsonValue.valueOf(1), encode(Collections.singletonList(1)));
    }

    @Test
    public final void decode_readsNormalList() {
        assertEquals(Arrays.asList(1, 2, 3), decode(new JsonArray().add(1).add(2).add(3)));
    }

    @Test
    public final void decode_readsValueAsSingleton() {
        assertEquals(Collections.singletonList(1), decode(JsonValue.valueOf(1)));
    }

    @Test
    public final void decode_readsMultidimensionalArray() {
        final JsonValue value = new JsonArray().add(new JsonArray().add(1).add(new JsonArray().add(2))).add(3);
        assertEquals(Arrays.asList(1, 2, 3), decode(value));
    }

    @Nullable
    public static JsonValue encode(final List<Integer> value) {
        return CODEC.encodeStart(HjsonOps.INSTANCE, value).get().left().orElse(null);
    }

    @Nullable
    private static List<Integer> decode(final JsonValue value) {
        final Pair<List<Integer>, JsonValue> pair = CODEC.decode(HjsonOps.INSTANCE, value).get().left().orElse(null);
        return pair != null ? pair.getFirst() : null;
    }
}
