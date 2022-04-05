package personthecat.catlib.serialization;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import personthecat.catlib.serialization.codec.AutoFlatListCodec;
import personthecat.catlib.serialization.codec.XjsOps;
import xjs.core.Json;
import xjs.core.JsonValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AutoFlatListCodecTest {

    private static final Codec<List<Integer>> CODEC = new AutoFlatListCodec<>(Codec.INT);

    @Test
    public final void encode_writesNormalList() {
        assertEquals(Json.array(1, 2, 3), encode(Arrays.asList(1, 2, 3)));
    }

    @Test
    public final void encode_writesSingletonAsValue() {
        assertEquals(Json.value(1), encode(Collections.singletonList(1)));
    }

    @Test
    public final void decode_readsNormalList() {
        assertEquals(Arrays.asList(1, 2, 3), decode(Json.array(1, 2, 3)));
    }

    @Test
    public final void decode_readsValueAsSingleton() {
        assertEquals(Collections.singletonList(1), decode(Json.value(1)));
    }

    @Test
    public final void decode_readsMultidimensionalArray() {
        final JsonValue value = Json.array().add(Json.array().add(1).add(Json.array().add(2))).add(3);
        assertEquals(Arrays.asList(1, 2, 3), decode(value));
    }

    @Nullable
    public static JsonValue encode(final List<Integer> value) {
        return CODEC.encodeStart(XjsOps.INSTANCE, value).get().left().orElse(null);
    }

    @Nullable
    private static List<Integer> decode(final JsonValue value) {
        final Pair<List<Integer>, JsonValue> pair = CODEC.decode(XjsOps.INSTANCE, value).get().left().orElse(null);
        return pair != null ? pair.getFirst() : null;
    }
}
