package personthecat.catlib.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
}
