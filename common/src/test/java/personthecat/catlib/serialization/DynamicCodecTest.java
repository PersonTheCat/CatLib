package personthecat.catlib.serialization;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static personthecat.catlib.serialization.CodecUtils.dynamic;
import static personthecat.catlib.serialization.DynamicField.extend;
import static personthecat.catlib.serialization.DynamicField.field;
import static personthecat.catlib.serialization.DynamicField.nullable;
import static personthecat.catlib.serialization.DynamicField.recursive;
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
        final JsonObject json = parse("");
        final SimpleObject o = decode(SimpleObject.CODEC, json);
        assertNotNull(o);
        assertNull(o.a);
        assertEquals(0, o.b);
    }

    @Test
    public void simpleCodec_doesNotSupportReadingNullValues() {
        final JsonObject json = parse("a:null");
        final SimpleObject o = decode(SimpleObject.CODEC, json);
        assertNull(o);
    }

    @Test
    public void simpleCodec_supportsWritingNullValues() {
        final SimpleObject o = new SimpleObject();
        final JsonObject json = encode(SimpleObject.CODEC, o);
        assertNotNull(json);
        assertTrue(json.get("a").isNull());
    }

    @Test
    public void nullableCodec_supportsReadingNullValues() {
        final JsonObject json = parse("a:null");
        final NullableObject o = decode(NullableObject.CODEC, json);
        assertNotNull(o);
        assertNull(o.a);
    }

    @Test
    public void nullableCodec_supportsReadingNonnullValues() {
        final JsonObject json = parse("a:test");
        final NullableObject o = decode(NullableObject.CODEC, json);
        assertNotNull(o);
        assertEquals("test", o.a);
    }

    @Test
    public void nullableCodec_supportsWritingNullValues() {
        final NullableObject o = new NullableObject();
        o.a = null;
        final JsonObject json = encode(NullableObject.CODEC, o);
        assertNotNull(json);
        assertTrue(json.get("a").isNull());
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
        final JsonObject json = parse("");
        final RecursiveObject o = decode(RecursiveObject.CODEC, json);
        assertNotNull(o);
        assertNull(o.a);
        assertNull(o.b);
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
        final JsonObject json = parse("");
        final ExtendingObject o = decode(ExtendingObject.CODEC, json);
        assertNotNull(o);
        assertNull(o.a);
        assertNotNull(o.i);
        assertEquals(0, o.i.b);
    }

    private static JsonObject parse(final String json) {
        return JsonObject.readHjson(json).asObject();
    }

    @Nullable
    private static <T> T decode(final Codec<T> codec, final JsonValue value) {
        final Pair<T, JsonValue> pair = codec.decode(HjsonOps.INSTANCE, value).get().left().orElse(null);
        return pair != null ? pair.getFirst() : null;
    }

    public static <T> JsonObject encode(final Codec<T> codec, final T value) {
        return codec.encodeStart(HjsonOps.INSTANCE, value).getOrThrow(false, e -> {}).asObject();
    }

    static class SimpleObject {
        @Nullable String a;
        int b;

        static Codec<SimpleObject> CODEC = dynamic(SimpleObject::new).create(
            field(Codec.STRING, "a", o -> o.a, (o, a) -> o.a = a),
            field(Codec.INT, "b", o -> o.b, (o, b) -> o.b = b)
        );
    }

    static class NullableObject {
        @Nullable String a = "nonnull";

        static Codec<NullableObject> CODEC = dynamic(NullableObject::new).create(
            nullable(Codec.STRING, "a", o -> o.a, (o, a) -> o.a = a)
        );
    }

    static class RecursiveObject {
        @Nullable String a;
        @Nullable RecursiveObject b;

        static Codec<RecursiveObject> CODEC = dynamic(RecursiveObject::new).create(
            field(Codec.STRING, "a", o -> o.a, (o, a) -> o.a = a),
            recursive("b", o -> o.b, (o, b) -> o.b = b)
        );
    }

    static class ExtendingObject {
        @Nullable String a;
        InnerObject i = new InnerObject();

        static Codec<ExtendingObject> CODEC = dynamic(ExtendingObject::new).create(
            field(Codec.STRING, "a", o -> o.a, (o, a) -> o.a = a),
            extend(InnerObject.CODEC, "i", o -> o.i, (o, i) -> o.i = i)
        );
    }

    static class InnerObject {
        int b;

        static Codec<InnerObject> CODEC = dynamic(InnerObject::new).create(
            field(Codec.INT, "b", o -> o.b, (o, b) -> o.b = b)
        );
    }
}
