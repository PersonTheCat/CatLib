package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import org.junit.jupiter.api.Test;
import xjs.data.Json;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaultTry;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.CapturingCodec.capture;
import static personthecat.catlib.serialization.codec.CapturingCodec.receive;
import static personthecat.catlib.serialization.codec.CapturingCodec.supply;

public class CapturingCodecTest {

    @Test
    public void decode_withValueAtReceiver_returnsSuccess() {
        final var expected = new TestSubject(List.of(
            new Entry("a", "b")
        ));
        final var result = parse(TestSubject.CAPTURE, """
            entries: [
              { required: 'a', receiver: 'b' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withValueAtCaptor_returnsSuccess() {
        final var expected = new TestSubject(List.of(
            new Entry("c", "d")
        ));
        final var result = parse(TestSubject.CAPTURE, """
            captor: 'd'
            entries: [
              { required: 'c' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withValuesAtCaptorAndReceiver_usesValueAtReceiver() {
        final var expected = new TestSubject(List.of(
            new Entry("b", "c")
        ));
        final var result = parse(TestSubject.CAPTURE, """
            captor: 'a'
            entries: [
              { required: 'b', receiver: 'c' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withValueAtCaptor_whenValueIsNotReceived_returnsError() {
        final var result = parse(TestSubject.CAPTURE, """
            required: 'x'
            entries: [
              { receiver: 'y' }
            ]
            """);
        assertError(result);
        assertContains(getMessage(result), "No key required");
    }

    @Test
    public void decode_whenValuesAreMissing_returnsError() {
        final var result = parse(TestSubject.CAPTURE, """
            entries: [{}]
            """);
        assertError(result);
        assertContains(getMessage(result), "No key required");
        assertContains(getMessage(result), "No key receiver");
    }

    @Test
    public void decode_withValueAtCaptor_andMultipleReceivers_returnsSuccess() {
        final var expected = new TestSubject(List.of(
            new Entry("c", "d"),
            new Entry("b", "d")
        ));
        final var result = parse(TestSubject.CAPTURE, """
            captor: 'd'
            entries: [
              { required: 'c' }
              { required: 'b' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withValueSupplied_whenReceiverIsAbsent_usesSuppliedValue() {
        final var expected = new TestSubject(List.of(
            new Entry("x", TestSubject.SUPPLIED_VALUE)
        ));
        final var result = parse(TestSubject.SUPPLY, """
            entries: [
              { required: 'x' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withValueSupplied_whenReceiverIsPresent_usesGivenValue() {
        final var expected = new TestSubject(List.of(
            new Entry("y", "z")
        ));
        final var result = parse(TestSubject.SUPPLY, """
            entries: [
              { required: 'y', receiver: 'z' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withOverride_whenDefaultedIsAbsent_usesOverrideValue() {
        final var expected = new TestSubject(List.of(
            new Entry("q", "r", TestSubject.OVERRIDE_VALUE)
        ));
        final var result = parse(TestSubject.OVERRIDE, """
            entries: [
              { required: 'q', receiver: 'r' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withOverride_whenDefaultedIsPresent_usesGivenValue() {
        final var expected = new TestSubject(List.of(
            new Entry("s", "t", "u")
        ));
        final var result = parse(TestSubject.OVERRIDE, """
            entries: [
              { required: 's', receiver: 't', defaulted: 'u' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDoubleOverride_whenDefaultedIsAbsent_usesTopOverride() {
        final var expected = new TestSubject(List.of(
            new Entry("g", "h", TestSubject.DOUBLE_OVERRIDE_VALUE)
        ));
        final var result = parse(TestSubject.DOUBLE_OVERRIDE, """
            entries: [
              { required: 'g', receiver: 'h' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withDoubleOverride_whenDefaultedIsPresent_usesGivenValue() {
        final var expected = new TestSubject(List.of(
            new Entry("p", "q", "r")
        ));
        final var result = parse(TestSubject.DOUBLE_OVERRIDE, """
            entries: [
              { required: 'p', receiver: 'q', defaulted: 'r' }
            ]
            """);
        assertSuccess(expected, result);
    }

    @Test
    public void decode_withoutOverride_whenDefaultedIsAbsent_usesOriginalDefault() {
        final var expected = new TestSubject(List.of(
            new Entry("l", "m", Entry.DEFAULT_VALUE)
        ));
        final var result = parse(TestSubject.CODEC, """
            entries: [
              { required: 'l', receiver: 'm' }
            ]
            """);
        assertSuccess(expected, result);
    }

    private static DataResult<TestSubject> parse(MapCodec<TestSubject> codec, String json) {
        return codec.codec().parse(XjsOps.INSTANCE, Json.parse(json));
    }

    private static <T> void assertSuccess(T expected, DataResult<T> actual) {
        if (!actual.isSuccess()) {
            assertionFailure().message(getMessage(actual)).actual("error").expected("success").buildAndThrow();
        }
        assertEquals(expected, actual.getOrThrow());
    }

    private static <T> void assertError(DataResult<T> actual) {
        if (!actual.isError()) {
            assertionFailure().message("parsed: " + actual.getOrThrow()).actual("success").expected("error").buildAndThrow();
        }
    }

    private static void assertContains(String s, String contains) {
        if (!s.contains(contains)) {
            assertionFailure().message("actual does not contain expected").actual(s).expected(contains).buildAndThrow();
        }
    }

    private static String getMessage(DataResult<?> result) {
        return result.error().orElseThrow().message();
    }

    private record TestSubject(List<Entry> entries) {
        private static final String SUPPLIED_VALUE = "supplied_value";
        private static final String OVERRIDE_VALUE = "override_value";
        private static final String DOUBLE_OVERRIDE_VALUE = "double_override_value";
        private static final MapCodec<TestSubject> CODEC =
            Entry.CODEC.codec().listOf().fieldOf("entries").xmap(TestSubject::new, TestSubject::entries);
        private static final MapCodec<TestSubject> CAPTURE =
            CapturingCodec.of(CODEC).capturing(capture("captor", Codec.STRING));
        private static final MapCodec<TestSubject> SUPPLY =
            CapturingCodec.of(CODEC).capturing(supply("captor", SUPPLIED_VALUE));
        private static final MapCodec<TestSubject> OVERRIDE =
            CapturingCodec.of(CODEC).capturing(supply("defaulted", OVERRIDE_VALUE));
        private static final MapCodec<TestSubject> DOUBLE_OVERRIDE =
            CapturingCodec.of(OVERRIDE).capturing(supply("defaulted", DOUBLE_OVERRIDE_VALUE));
    }

    private record Entry(String required, String receiver, String defaulted) {
        private static final String DEFAULT_VALUE = "default_value";
        private static final MapCodec<Entry> CODEC = codecOf(
            field(Codec.STRING, "required", Entry::required),
            defaultTry(Codec.STRING, "receiver", receive("captor"), Entry::receiver),
            defaultTry(Codec.STRING, "defaulted", receive("defaulted", DEFAULT_VALUE), Entry::defaulted),
            Entry::new
        );
        private Entry(String required, String receiver) {
            this(required, receiver, DEFAULT_VALUE);
        }
    }
}
