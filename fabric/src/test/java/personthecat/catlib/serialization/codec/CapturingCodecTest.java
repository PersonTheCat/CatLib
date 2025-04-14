package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import org.junit.jupiter.api.Test;
import xjs.data.Json;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.CapturingCodec.captor;
import static personthecat.catlib.serialization.codec.CapturingCodec.receiver;

public class CapturingCodecTest {

    @Test
    public void decode_withValuesAtReceiver_returnsSuccess() {
        final var expected = new TestSubject(List.of(
            new Entry(1, 2)
        ));
        final var result = TestSubject.parse("""
            entries: [
              { count: 1, radius: 2 }
            ]
            """);
        assertTrue(result.isSuccess());
        assertEquals(expected, result.getOrThrow());
    }

    @Test
    public void decode_withValuesAtCaptor_returnsSuccess() {
        final var expected = new TestSubject(List.of(
            new Entry(3, 4)
        ));
        final var result = TestSubject.parse("""
            radius: 4
            entries: [
              { count: 3 }
            ]
            """);
        assertTrue(result.isSuccess());
        assertEquals(expected, result.getOrThrow());
    }

    @Test
    public void decode_withValuesAtCaptorAndReceiver_usesValuesAtReceiver() {
        final var expected = new TestSubject(List.of(
            new Entry(2, 3)
        ));
        final var result = TestSubject.parse("""
            radius: 1
            entries: [
              { count: 2, radius: 3 }
            ]
            """);
        assertTrue(result.isSuccess());
        assertEquals(expected, result.getOrThrow());
    }

    @Test
    public void decode_withValuesAtCaptor_whenValuesAreFiltered_returnsError() {
        final var result = TestSubject.parse("""
            count: 1
            entries: [
              { radius: 2 }
            ]
            """);
        assertTrue(result.isError());
    }

    @Test
    public void decode_whenValuesAreMissing_returnsError() {
        final var result = TestSubject.parse("""
            entries: [{}]
            """);
        assertTrue(result.isError());
    }

    private record TestSubject(List<Entry> entries) {
        private static final MapCodec<TestSubject> CODEC = captor(
            Entry.CODEC.codec().listOf().fieldOf("entries").xmap(TestSubject::new, TestSubject::entries),
            List.of("radius")
        );

        private static DataResult<TestSubject> parse(String json) {
            return CODEC.codec().parse(XjsOps.INSTANCE, Json.parse(json));
        }
    }

    private record Entry(int count, int radius) {
        private static final MapCodec<Entry> CODEC = receiver(codecOf(
            field(Codec.INT, "count", Entry::count),
            field(Codec.INT, "radius", Entry::radius),
            Entry::new
        ));
    }
}
