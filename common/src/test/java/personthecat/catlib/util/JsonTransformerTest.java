package personthecat.catlib.util;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.junit.jupiter.api.Test;
import personthecat.catlib.util.JsonTransformer.ObjectResolver;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JsonTransformerTest {

    @Test
    public void rootResolver_resolvesRootPathOnly() {
        final JsonObject subject = parse("a:{x:1},b:{y:2},c:{z:3}");
        final List<JsonObject> resolved = new ArrayList<>();
        JsonTransformer.root().forEach(subject, resolved::add);

        assertEquals(1, resolved.size());
        assertSame(subject, resolved.get(0));
    }

    @Test
    public void staticResolver_resolvesRegularPaths() {
        final JsonObject subject = parse("a:[{x:1},{y:2}],b:[{z:3}]");
        final List<JsonObject> resolved = new ArrayList<>();
        JsonTransformer.withPath("a").forEach(subject, resolved::add);

        assertEquals(2, resolved.size());
        assertTrue(resolved.contains(parse("x:1")));
        assertTrue(resolved.contains(parse("y:2")));
    }

    @Test
    public void recursiveResolver_resolvesPathsRecursively() {
        final JsonObject subject = parse("a:[{z:{z:{z:{}}}},{z:{}}],z:{}");
        final List<JsonObject> resolved = new ArrayList<>();
        JsonTransformer.recursive("z").forEach(subject, resolved::add);

        assertEquals(5, resolved.size());
    }

    @Test
    public void history_renamesAllMatches() {
        final JsonObject transformed = parse("a:[{x:1},{y:2},{z:3}]");
        JsonTransformer.withPath("a").history("x", "y", "z", "b").updateAll(transformed);

        assertEquals(parse("a:[{b:1},{b:2},{b:3}]"), transformed);
    }

    @Test
    public void collapse_removesNestedObject() {
        final JsonObject transformed = parse("a:{outer:{inner:{b:1}}}");
        JsonTransformer.withPath("a").collapse("outer", "inner").updateAll(transformed);

        assertEquals(parse("a:{outer:{b:1}}"), transformed);
    }

    @Test
    public void toRange_convertsToRange() {
        final JsonObject transformed = parse("a:{min:1,max:2}");
        JsonTransformer.withPath("a").toRange("min", 1, "max", 2, "val").updateAll(transformed);

        assertEquals(parse("a:{val:[1,2]}"), transformed);
    }

    @Test
    public void toRange_suppliesDefaultValue() {
        final JsonObject transformed = parse("a:{max:2}");
        JsonTransformer.withPath("a").toRange("min", 1, "max", 2, "val").updateAll(transformed);

        assertEquals(parse("a:{val:[1,2]}"), transformed);
    }

    @Test
    public void toRange_simplifiesConstant() {
        final JsonObject transformed = parse("a:{min:1,max:1}");
        JsonTransformer.withPath("a").toRange("min", 1, "max", 2, "val").updateAll(transformed);

        assertEquals(parse("a:{val:1}"), transformed);
    }

    @Test
    public void markRemoved_addsComment() {
        final JsonObject transformed = parse("a:{b:true}");
        JsonTransformer.withPath("a").markRemoved("b", "1.0").updateAll(transformed);

        assertEquals(parse("a:{#Removed in 1.0. You can delete this field.\nb:true}"), transformed);
    }

    @Test
    public void renameValue_renamesMatchingValue() {
        final JsonObject transformed = parse("a:{b:'dog'}");
        JsonTransformer.withPath("a").renameValue("b", "dog", "cat").updateAll(transformed);

        assertEquals(parse("a:{b:'cat'}"), transformed);
    }

    @Test
    public void renameValue_doesNotRenameMismatch() {
        final JsonObject transformed = parse("a:{b:'horse'}");
        JsonTransformer.withPath("a").renameValue("b", "dog", "cat").updateAll(transformed);

        assertEquals(parse("a:{b:'horse'}"), transformed);
    }

    @Test
    public void transform_appliesFunction() {
        final JsonObject transformed = parse("a:{b:'c'}");
        JsonTransformer.withPath("a").transform("b", (k, v) -> Pair.of("k",  JsonValue.valueOf("v"))).updateAll(transformed);

        assertEquals(parse("a:{k:'v'}"), transformed);
    }

    @Test
    public void ifPresent_runsIfPresent() {
        final JsonObject transformed = parse("a:{b:'c'}");
        final MutableBoolean updated = new MutableBoolean(false);
        JsonTransformer.withPath("a").ifPresent("b", (o, v) -> updated.setTrue()).updateAll(transformed);

        assertTrue(updated.getValue());
    }

    @Test
    public void ifPresent_doesNotRunIfAbsent() {
        final JsonObject transformed = parse("a:{}");
        final MutableBoolean updated = new MutableBoolean(false);
        JsonTransformer.withPath("a").ifPresent("b", (o, v) -> updated.setTrue()).updateAll(transformed);

        assertFalse(updated.getValue());
    }

    @Test
    public void moveArray_mergesTwoArrays() {
        final JsonObject transformed = parse("a:{b:[2],c:[1]}");
        JsonTransformer.withPath("a").moveArray("b", "c").updateAll(transformed);

        assertEquals(parse("a:{c:[1,2]}"), transformed);
    }

    @Test
    public void relocate_movesSimpleValue() {
        final JsonObject transformed = parse("a:{b:{c:24}}");
        JsonTransformer.root().relocate("a.b.c", "d.e.f").updateAll(transformed);

        assertEquals(parse("a:{b:{}},d:{e:{f:24}}"), transformed);
    }

    @Test
    public void relocate_mergesArrayValues() {
        final JsonObject transformed = parse("a:{b:[4,5,6]},c:[1,2,3]");
        JsonTransformer.root().relocate("a.b", "c").updateAll(transformed);

        assertEquals(parse("a:{},c:[1,2,3,4,5,6]"), transformed);
    }

    @Test
    public void relocate_mergesObjectValues() {
        final JsonObject transformed = parse("a:{b:{x:4,y:5,z:6}},c:{i:1,j:2,k:3}");
        JsonTransformer.root().relocate("a.b", "c").updateAll(transformed);

        assertEquals(parse("a:{},c:{i:1,j:2,k:3,x:4,y:5,z:6}"), transformed);
    }

    @Test
    public void relocate_ignoresIncompletePaths() {
        final JsonObject transformed = parse("a:{b:1}");
        JsonTransformer.root().relocate("a.b.c.d", "e").updateAll(transformed);

        assertEquals(parse("a:{b:1}"), transformed);
    }

    @Test
    public void setDefaults_updatesObjectRecursively() {
        final JsonObject transformed = parse("a:1,n:{}");
        final JsonObject defaults = parse("a:1,n:{k:9},b:2");
        JsonTransformer.root().setDefaults(defaults).updateAll(transformed);

        assertEquals(parse("a:1,n:{k:9},b:2"), transformed);
    }

    @Test
    public void setDefaults_doesNotReplaceUpdatedValues() {
        final JsonObject transformed = parse("a:1,b:2,c:3");
        final JsonObject defaults = parse("a:4,b:5,c:6");
        JsonTransformer.root().setDefaults(defaults).updateAll(transformed);

        assertEquals(parse("a:1,b:2,c:3"), transformed);
    }

    @Test
    public void remove_removesMatchingValue() {
        final JsonObject transformed = parse("a:1,b:2,c:3");
        JsonTransformer.root().remove("b", 2).updateAll(transformed);

        assertEquals(parse("a:1,c:3"), transformed);
    }

    @Test
    public void remove_ignoresMismatch() {
        final JsonObject transformed = parse("a:1,b:2,c:3");
        JsonTransformer.root().remove("b", 1).updateAll(transformed);

        assertEquals(parse("a:1,b:2,c:3"), transformed);
    }

    @Test
    public void removeAll_removesMultipleValues() {
        final JsonObject transformed = parse("a:1,b:2,c:3");
        JsonTransformer.root().remove(parse("b:2,c:3")).updateAll(transformed);

        assertEquals(parse("a:1"), transformed);
    }

    @Test
    public void removeNull_removesAnyValue() {
        final JsonObject transformed = parse("a:1,b:2,c:3");
        JsonTransformer.root().remove("b").updateAll(transformed);

        assertEquals(parse("a:1,c:3"), transformed);
    }

    @Test
    public void parentTransformer_appliesNestedTransformations() {
        final JsonObject transformed = parse("a:1");
        final ObjectResolver nested = JsonTransformer.root().history("a", "b");
        JsonTransformer.root().include(nested).updateAll(transformed);

        assertEquals(parse("b:1"), transformed);
    }

    @Test
    public void frozenTransformer_isImmutable() {
        final ObjectResolver transformer = JsonTransformer.root().freeze();

        assertThrows(UnsupportedOperationException.class, () -> transformer.relocate("", ""));
    }

    private static JsonObject parse(final String json) {
        return JsonValue.readHjson(json).asObject();
    }
}
