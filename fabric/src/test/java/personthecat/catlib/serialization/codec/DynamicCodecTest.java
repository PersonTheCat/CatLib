package personthecat.catlib.serialization.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import personthecat.catlib.serialization.json.JsonTransformer;
import xjs.data.Json;
import xjs.data.JsonObject;
import xjs.data.JsonValue;
import xjs.data.PathFilter;

import java.util.Collections;
import java.util.Objects;

import static personthecat.catlib.serialization.codec.CodecUtils.dynamic;
import static personthecat.catlib.serialization.codec.DynamicField.extend;
import static personthecat.catlib.serialization.codec.DynamicField.field;
import static personthecat.catlib.serialization.codec.DynamicField.nullable;
import static personthecat.catlib.serialization.codec.DynamicField.recursive;
import static personthecat.catlib.serialization.codec.DynamicField.required;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DynamicCodecTest {

    @Test
    public void simpleCodec_mapsValues() {
        final JsonObject json = parse("a:'test',b:1337");
        final SimpleObject o = decode(SimpleObject.CODEC, json);
        assertNotNull(o);
        assertEquals("test", o.a);
        assertEquals(1337, o.b);
    }

    @Test
    public void simpleCodec_supportsMissingValues() {
        final JsonObject json = Json.object();
        final SimpleObject o = decode(SimpleObject.CODEC, json);
        assertNotNull(o);
        assertNull(o.a);
        assertEquals(0, o.b);
    }

    @Test
    public void simpleCodec_supportsReadingNullValues() {
        final JsonObject json = parse("a:null");
        final SimpleObject o = decode(SimpleObject.CODEC, json);
        assertNotNull(o);
        assertNull(o.a);
        assertEquals(0, o.b);
    }

    @Test
    public void simpleCodec_supportsWritingNonnullValues() {
        final SimpleObject o = new SimpleObject();
        o.a = "test";
        o.b = 1337;
        final JsonObject json = encode(SimpleObject.CODEC, o);
        assertNotNull(json);
        assertEquals(Json.value("test"), json.get("a"));
        assertEquals(Json.value(1337), json.get("b"));
    }

    @Test
    public void simpleCodec_supportsWritingNullValues() {
        final SimpleObject o = new SimpleObject();
        final JsonObject json = encode(SimpleObject.CODEC, o);
        assertNotNull(json);
        assertNull(json.get("a"));
    }

    @Test
    public void simpleCodec_doesNotAccessExtraneousFields() {
        final JsonObject json = parse("a:'test',b:1337,c:'unused'");
        final SimpleObject o = decode(SimpleObject.CODEC, json);
        assertNotNull(o);
        assertEquals(Collections.singletonList("c"), json.getPaths(PathFilter.UNUSED));
    }

    @Test
    public void nullableCodec_supportsReadingNonnullValues() {
        final JsonObject json = parse("a:'test'");
        final NullableObject o = decode(NullableObject.CODEC, json);
        assertNotNull(o);
        assertEquals("test", o.a);
    }

    @Test
    public void nullableCodec_supportsReadingNullValues() {
        final JsonObject json = parse("a:null");
        final NullableObject o = decode(NullableObject.CODEC, json);
        assertNotNull(o);
        assertNull(o.a);
    }

    @Test
    public void nullableCodec_supportsWritingNonnullValues() {
        final NullableObject o = new NullableObject();
        o.a = "test";
        final JsonObject json = encode(NullableObject.CODEC, o);
        assertNotNull(json);
        assertEquals(Json.value("test"), json.get("a"));
    }

    @Test
    public void nullableCodec_supportsWritingNullValues() {
        final NullableObject o = new NullableObject();
        o.a = null;
        final JsonObject json = encode(NullableObject.CODEC, o);
        assertNotNull(json);
        assertNull(json.get("a"));
    }

    @Test
    public void requiredCodec_canMapValues() {
        final JsonObject json = parse("a:'test'");
        final RequiredObject o = decode(RequiredObject.CODEC, json);
        assertNotNull(o);
        assertEquals("test", o.a);
    }

    @Test
    public void requiredCodec_requiresFields() {
        final JsonObject json = Json.object();
        final RequiredObject o = decode(RequiredObject.CODEC, json);
        assertNull(o);
    }

    @Test
    public void requiredCodec_supportsWritingNonnullValues() {
        final RequiredObject o = new RequiredObject();
        o.a = "test";
        final JsonObject json = encode(RequiredObject.CODEC, o);
        assertNotNull(json);
        assertEquals(Json.value("test"), json.get("a"));
    }

    @Test
    public void recursiveCodec_mapsRecursively() {
        final JsonObject json = parse("a:'t1',b:{a:'t2',b:{a:'t3'}}");
        final RecursiveObject o = decode(RecursiveObject.CODEC, json);
        assertNotNull(o);
        assertEquals("t1", o.a);
        assertNotNull(o.b);
        assertEquals("t2", o.b.a);
        assertNotNull(o.b.b);
        assertEquals("t3", o.b.b.a);
        assertNull(o.b.b.b);
    }

    @Test
    public void recursiveCodec_supportsTerminalValues() {
        final JsonObject json = Json.object();
        final RecursiveObject o = decode(RecursiveObject.CODEC, json);
        assertNotNull(o);
        assertNull(o.a);
        assertNull(o.b);
    }

    @Test
    public void recursiveCodec_supportsWritingNonnullValues() {
        final RecursiveObject o = new RecursiveObject();
        o.a = "t1";
        o.b = new RecursiveObject();
        o.b.a = "t2";
        o.b.b = new RecursiveObject();
        o.b.b.a = "t3";
        final JsonObject json = encode(RecursiveObject.CODEC, o);
        assertNotNull(json);
        JsonTransformer.all().sort().updateAll(json);
        assertEquals(parse("a:'t1',b:{a:'t2',b:{a:'t3'}}"), json.unformatted());
    }

    @Test
    public void extendingCodec_simulatesInheritance() {
        final JsonObject json = parse("a:'test',b:1337");
        final ExtendingObject o = decode(ExtendingObject.CODEC, json);
        assertNotNull(o);
        assertEquals("test", o.a);
        assertNotNull(o.i);
        assertEquals(1337, o.i.b);
    }

    @Test
    public void extendingCodec_toleratesMissingField() {
        final JsonObject json = Json.object();
        final ExtendingObject o = decode(ExtendingObject.CODEC, json);
        assertNotNull(o);
        assertNull(o.a);
        assertNotNull(o.i);
        assertEquals(0, o.i.b);
    }

    @Test
    public void extendingCodec_supportsWritingNonnullValues() {
        final ExtendingObject o = new ExtendingObject();
        o.a = "test";
        o.i.b = 1337;
        final JsonObject json = encode(ExtendingObject.CODEC, o);
        assertNotNull(json);
        assertEquals(Json.value("test"), json.get("a"));
        assertEquals(Json.value(1337), json.get("b"));
    }

    @Test
    public void extendingCodec_doesNotSupportWritingNonnullValues() {
        final ExtendingObject o = new ExtendingObject();
        o.i = null;
        final JsonObject json = assertDoesNotThrow(() -> encode(ExtendingObject.CODEC, o));
        assertTrue(json.isEmpty());
    }

    @Test
    public void filteredCodec_whenFilterDoesPass_outputsField() {
        final FilteredObject o = new FilteredObject();
        o.a = 1234;
        final JsonObject json = assertDoesNotThrow(() -> encode(FilteredObject.CODEC, o));
        assertEquals(1234, Objects.requireNonNull(json.get("a")).asInt());
    }

    @Test
    public void filteredCodec_whenFilterDoesNotPass_doesNotOutputField() {
        final FilteredObject o = new FilteredObject();
        o.a = FilteredObject.FILTERED_VALUE;
        final JsonObject json = assertDoesNotThrow(() -> encode(FilteredObject.CODEC, o));
        assertNull(json.get("a"));
    }

    private static JsonObject parse(final String json) {
        return Json.parse(json).unformatted().asObject();
    }

    @Nullable
    private static <T> T decode(final MapCodec<T> codec, final JsonValue value) {
        final Pair<T, JsonValue> pair = codec.codec().decode(XjsOps.INSTANCE, value).result().orElse(null);
        return pair != null ? pair.getFirst() : null;
    }

    public static <T> JsonObject encode(final MapCodec<T> codec, final T value) {
        final JsonValue json = codec.codec().encodeStart(XjsOps.INSTANCE, value).result().orElse(null);
        return json != null ? json.asObject() : null;
    }

    static class SimpleObject {
        @Nullable String a;
        int b;

        static final MapCodec<SimpleObject> CODEC = dynamic(SimpleObject::new).create(
            field(Codec.STRING, "a", o -> o.a, (o, a) -> o.a = a),
            field(Codec.INT, "b", o -> o.b, (o, b) -> o.b = b)
        );
    }

    static class NullableObject {
        @Nullable String a = "nonnull";

        static final MapCodec<NullableObject> CODEC = dynamic(NullableObject::new).create(
            nullable(Codec.STRING, "a", o -> o.a, (o, a) -> o.a = a)
        );
    }

    static class RequiredObject {
        @NotNull String a = "nonnull";

        static final MapCodec<RequiredObject> CODEC = dynamic(RequiredObject::new).create(
            required(Codec.STRING, "a", o -> o.a, (o, a) -> o.a = a)
        );
    }

    static class RecursiveObject {
        @Nullable String a;
        @Nullable RecursiveObject b;

        static final MapCodec<RecursiveObject> CODEC = dynamic(RecursiveObject::new).create(
            field(Codec.STRING, "a", o -> o.a, (o, a) -> o.a = a),
            recursive("b", o -> o.b, (o, b) -> o.b = b)
        );
    }

    static class ExtendingObject {
        @Nullable String a;
        InnerObject i = new InnerObject();

        static final MapCodec<ExtendingObject> CODEC = dynamic(ExtendingObject::new).create(
            field(Codec.STRING, "a", o -> o.a, (o, a) -> o.a = a),
            extend(InnerObject.CODEC, "i", o -> o.i, (o, i) -> o.i = i)
        );
    }

    static class InnerObject {
        int b;

        static final MapCodec<InnerObject> CODEC = dynamic(InnerObject::new).create(
            field(Codec.INT, "b", o -> o.b, (o, b) -> o.b = b)
        );
    }

    static class FilteredObject {
        int a;

        static final int FILTERED_VALUE = Integer.MAX_VALUE;
        static final MapCodec<FilteredObject> CODEC = dynamic(FilteredObject::new).create(
            DynamicField.<FilteredObject, FilteredObject, Integer>field(
                    Codec.INT, "a", o -> o.a, (o, a) -> o.a = a)
                .withOutputFilter(a -> a != FILTERED_VALUE)
        );
    }
}
