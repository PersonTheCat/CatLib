package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.filter;

import static personthecat.catlib.test.TestUtils.assertSuccess;
import static personthecat.catlib.test.TestUtils.encode;

public class FilteredCodecTest {

    @Test
    public void filteredCodec_whenFilterPasses_encodesData() {
        final var data = new Conditional(true, "Hello, world!");
        final var result = encode(Conditional.FILTERED, data);
        assertSuccess(Map.of("enabled", true, "value", "Hello, world!"), result);
    }

    @Test
    public void filteredCodec_whenFilterFails_doesNotEncodeData() {
        final var data = new Conditional(false, "Goodbye, cruel world!");
        final var result = encode(Conditional.FILTERED, data);
        assertSuccess(Map.of(), result);
    }

    private record Conditional(boolean enabled, String value) {
        private static final MapCodec<Conditional> CODEC = codecOf(
            FieldDescriptor.defaulted(Codec.BOOL, "enabled", false, Conditional::enabled),
            FieldDescriptor.defaulted(Codec.STRING, "value", "", Conditional::value),
            Conditional::new
        );
        private static final MapCodec<Conditional> FILTERED =
            filter(CODEC, c -> c.enabled);
    }
}
