package personthecat.catlib.serialization.codec.context;

import com.mojang.datafixers.util.Either;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import personthecat.catlib.serialization.codec.XjsOps;
import personthecat.catlib.test.McBootstrapExtension;
import xjs.data.Json;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(McBootstrapExtension.class)
public class ContextualOpsTest {

    @Test
    public void noRecursion_noErrors_producesEmptyContext() {
        final var ctx = parseGetContext("type: 'add', argument1: 1, argument2: 2");
        assertTrue(ctx.getErrors().isEmpty());
        assertTrue(ctx.getErrorRoot().messageSuppliers.isEmpty());
        assertFalse(ctx.getErrorRoot().hasNestedErrors());
    }

    @Test
    public void noRecursion_singleError_producesRootError() {
        final var ctx = parseGetContext("type: 'add', argument1: false, argument2: 2");

        final var root = ctx.getErrorRoot();
        final var rootErrors = new ArrayList<>(root.messages.get());
        assertEquals(1, rootErrors.size());
        assertTrue(rootErrors.contains("Not an XJS object: false; Not a number: false; Not a number: {\"type\":\"add\",\"argument1\":false,\"argument2\":2}"));
    }

    @Test
    public void noRecursion_singleError_producesLeafErrors() {
        final var ctx = parseGetContext("type: 'add', argument1: false, argument2: 2");

        final var errors = new ArrayList<>(ctx.getErrors());
        assertEquals(1, errors.size()); // one category (null)

        final var leaves = errors.getFirst().erredLeaves();
        assertEquals(1, leaves.size()); // one leaf (argument1)

        final var leaf = leaves.get(List.of(Either.left("argument1")));
        assertNotNull(leaf);
        assertTrue(leaf.contains("Not a number: false"));
        assertTrue(leaf.contains("Not an XJS object: false"));
    }

    @Test
    public void noRecursion_multipleErrors_producesMultipleLeafErrors() {
        final var ctx = parseGetContext("type: 'add', argument1: false, argument2: true");

        final var errors = new ArrayList<>(ctx.getErrors());
        assertEquals(1, errors.size()); // one category (null)

        final var leaves = errors.getFirst().erredLeaves();
        assertEquals(2, leaves.size()); // two leaves (argument1, argument2)

        final var argument1 = leaves.get(List.of(Either.left("argument1")));
        assertNotNull(argument1);
        assertTrue(argument1.contains("Not a number: false"));
        assertTrue(argument1.contains("Not an XJS object: false"));

        final var argument2 = leaves.get(List.of(Either.left("argument2")));
        assertNotNull(argument2);
        assertTrue(argument2.contains("Not a number: true"));
        assertTrue(argument2.contains("Not an XJS object: true"));
    }

    @Test
    public void recursion_nestedError_producesRootError() {
        final var ctx = parseGetContext("type: 'add', argument1: 1, argument2: { type: 'mul', argument1: 'bananas', argument2: 2 }");

        final var root = ctx.getErrorRoot();
        final var rootErrors = new ArrayList<>(root.messages.get());
        assertEquals(1, rootErrors.size());
        assertTrue(rootErrors.contains("Not an XJS object: bananas; Not a number: bananas; Not a number: {\"type\":\"mul\",\"argument1\":\"bananas\",\"argument2\":2}; Not a number: {\"type\":\"add\",\"argument1\":1,\"argument2\":{\"type\":\"mul\",\"argument1\":\"bananas\",\"argument2\":2}}"));
    }

    @Test
    public void recursion_nestedError_producesLeafError() {
        final var ctx = parseGetContext("type: 'add', argument1: 1, argument2: { type: 'mul', argument1: 'bananas', argument2: 2 }");

        final var errors = new ArrayList<>(ctx.getErrors());
        assertEquals(1, errors.size()); // one category (null)

        final var leaves = errors.getFirst().erredLeaves();
        assertEquals(1, leaves.size()); // one leaf (argument2.argument1)

        final var leaf = leaves.get(List.of(Either.left("argument2"), Either.left("argument1")));
        assertNotNull(leaf);
        assertTrue(leaf.contains("Not a number: bananas"));
        assertTrue(leaf.contains("Not an XJS object: bananas"));
    }

    private static DecodeContext parseGetContext(String densityJson) {
        final var ops = ContextualOps.create(XjsOps.INSTANCE);
        DensityFunction.HOLDER_HELPER_CODEC.parse(ops, Json.parse(densityJson))
            .ifError(ops.catlib$getContext()::reportError);
        return ops.catlib$getContext();
    }
}
