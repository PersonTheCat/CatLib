package personthecat.catlib.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.hjson.JsonLiteral;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonPathTest {

    @Test
    public void parse_readsBothKeysAndIndices() throws CommandSyntaxException {
        final JsonPath expected = JsonPath.builder().key("key").index(0).key("hello").key("world").index(1).build();
        final String raw = "key[0].hello.world[1]";
        final JsonPath path = JsonPath.parse(raw);
        assertEquals(expected, path);
    }

    @Test
    public void serialize_printsCorrectPath() {
        final String expected = "key.hello[1][0]";
        final JsonPath path = JsonPath.builder().key("key").key("hello").index(1).index(0).build();
        assertEquals(expected, path.toString());
    }

    @Test
    public void toPaths_convertsComplexPaths() {
        final JsonObject subject = parse("a:{b:{}},c:[[{d:{}}],[{e:{}}]]");

        final List<JsonPath> expected = Arrays.asList(
            JsonPath.builder().key("a").build(),
            JsonPath.builder().key("a").key("b").build(),
            JsonPath.builder().key("c").build(),
            JsonPath.builder().key("c").index(0).build(),
            JsonPath.builder().key("c").index(0).index(0).build(),
            JsonPath.builder().key("c").index(0).index(0).key("d").build(),
            JsonPath.builder().key("c").index(1).build(),
            JsonPath.builder().key("c").index(1).index(0).build(),
            JsonPath.builder().key("c").index(1).index(0).key("e").build()
        );
        assertEquals(expected, JsonPath.getAllPaths(subject));
    }

    @Test
    public void getLastContainer_returnsParent() {
        final JsonObject json = parse("a:{b:{c:{}}}");
        final JsonPath path = JsonPath.builder().key("a").key("b").key("c").build();

        final JsonObject get = path.getLastContainer(json).asObject();
        assertNotNull(get.get("c"));
    }

    @Test
    public void setValue_setsValueInLastContainer() {
        final JsonObject json = parse("a:{b:{c:{}}}");
        final JsonPath path = JsonPath.builder().key("a").key("b").key("c").build();
        final JsonObject expected = parse("a:{b:{c:true}}");

        path.setValue(json, JsonLiteral.jsonTrue());
        assertEquals(expected, json);
    }

    @Test
    public void setValue_doesNotDestroyData() {
        final JsonObject json = parse("a:{b:{c:{}}}");
        final JsonPath path = JsonPath.builder().key("a").index(0).build();
        final JsonObject out = (JsonObject) json.deepCopy();

        path.setValue(out, JsonLiteral.jsonTrue());
        assertEquals(json, out);
    }

    @Test
    public void getClosestMatch_withSamePath_locatesMatch() {
        final JsonObject subject = parse("a:{b:{}},c:[[{d:{}}]]");

        final JsonPath canonical = JsonPath.builder().key("c").index(0).build();
        final JsonPath match = canonical.getClosestMatch(subject);
        assertEquals(canonical, match);
        assertNotSame(canonical, match);
    }

    @Test
    public void getClosestMatch_withSimilarPath_locatesMatch() {
        final JsonObject subject = parse("a:{b:{}},c:[[{d:{}}]]");

        final JsonPath canonical = JsonPath.builder().key("a").index(0).key("b").build();
        final JsonPath expected = JsonPath.builder().key("a").key("b").build();
        assertEquals(expected, canonical.getClosestMatch(subject));
    }

    @Test
    public void getClosestMatch_withNestedArrays_locatesMatch() {
        final JsonObject subject = parse("a:{b:{}},c:[[{d:{}}]]");

        final JsonPath canonical = JsonPath.builder().key("c").index(0).index(0).index(0).key("d").build();
        final JsonPath expected = JsonPath.builder().key("c").index(0).index(0).key("d").build();
        assertEquals(expected, canonical.getClosestMatch(subject));
    }

    @Test
    public void getClosestMatch_withNoMatch_generatesOriginalPath() {
        final JsonObject subject = parse("a:{b:{}},c:[[{d:{}}]]");

        final JsonPath canonical = JsonPath.builder().key("c").index(1).key("d").build();
        assertEquals(canonical, canonical.getClosestMatch(subject));
    }

    @Test
    public void getClosestMatch_whenCanonicalIsShorterThanActual_locatesMatch() {
        final JsonObject subject = parse("a:{b:{}},c:[[{d:{}}]]");

        final JsonPath canonical = JsonPath.builder().key("c").key("d").build();
        final JsonPath expected = JsonPath.builder().key("c").index(0).index(0).key("d").build();
        assertEquals(expected, canonical.getClosestMatch(subject));
    }

    @Test
    public void getClosestMatch_withPartialPath_matchesPartially() {
        final JsonObject subject = parse("a:{b:''}");

        final JsonPath canonical = JsonPath.builder().key("a").index(0).key("b").key("c").build();
        final JsonPath expected = JsonPath.builder().key("a").key("b").key("c").build();
        assertEquals(expected, canonical.getClosestMatch(subject));
    }

    @Test
    public void getClosestMatch_withPartialPath_missingKey_matchesPartially() {
        final JsonObject subject = parse("a:{b:{c:{}}}");

        final JsonPath canonical = JsonPath.builder().key("a").key("b").index(0).key("c").key("d").build();
        final JsonPath expected = JsonPath.builder().key("a").key("b").key("c").key("d").build();
        assertEquals(expected, canonical.getClosestMatch(subject));
    }

    @Test
    public void getLastAvailable_whenFullPathIsAvailable_returnsEnd() {
        final JsonObject subject = parse("a:{b:[{c:{}}]}");
        final JsonPath path = JsonPath.builder().key("a").key("b").index(0).key("c").build();

        assertEquals(path.size() - 1, path.getLastAvailable(subject));
    }

    @Test
    public void getLastAvailable_whenPartialPathIsPresent_returnsLastAvailable() {
        final JsonObject subject = parse("a:{b:[]}");
        final JsonPath path = JsonPath.builder().key("a").key("b").index(0).key("c").build();

        assertEquals(1, path.getLastAvailable(subject));
    }

    @Test
    public void getLastAvailable_whenNoneIsPresent_returnsNegativeOne() {
        final JsonObject subject = parse("a:{b:[]}");
        final JsonPath path = JsonPath.builder().key("x").key("y").key("z").build();

        assertEquals(-1, path.getLastAvailable(subject));
    }

    @Test
    public void subPath_generatesPathSlice() {
        final JsonPath expected = JsonPath.builder().key("two").index(3).build();
        final JsonPath path = JsonPath.builder().index(1).key("two").index(3).key("four").build();
        assertEquals(expected, path.subPath(1, 3));
    }

    @Test
    public void append_addsElementsFromOtherPath() {
        final JsonPath pathA = JsonPath.builder().key("a").index(0).build();
        final JsonPath pathB = JsonPath.builder().key("b").index(1).build();
        final JsonPath expected = JsonPath.builder().key("a").index(0).key("b").index(1).build();

        assertEquals(expected, pathA.append(pathB));
    }

    @Test
    public void builder_isNavigable() {
        final JsonPath expected = JsonPath.builder().key("k1").key("k2").build();
        final JsonPath path = JsonPath.builder().key("k1").index(0).build();
        assertEquals(expected, path.toBuilder().up().key("k2").build());
    }

    @Test
    public void builder_toleratesOverNavigation() {
        final JsonPath path = JsonPath.builder().key("key").build();
        final JsonPath result = assertDoesNotThrow(() -> path.toBuilder().up(100).build());
        assertTrue(result.isEmpty());
    }

    @Test
    public void stub_isImmutable() {
        final JsonPath.Stub stub = JsonPath.stub();
        assertNotSame(stub, stub.key("immutable"));
    }

    @Test
    public void stub_generatesValidPath() {
        final JsonPath.Stub stub = JsonPath.stub().key("a").key("b").index(3).key("d");
        assertEquals("a.b[3].d", stub.capture().toString());
    }

    private static JsonObject parse(final String json) {
        return JsonValue.readHjson(json).asObject();
    }
}
