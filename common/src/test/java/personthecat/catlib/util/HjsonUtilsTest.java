package personthecat.catlib.util;

import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.junit.jupiter.api.Test;
import personthecat.catlib.data.JsonPath;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class HjsonUtilsTest {

    @Test
    public void filter_generatesObject_withGivenPathsOnly() {
        final JsonObject json = parse("a:{},b:{},c:{},d:[{},{},{},{e:{},f:{}},{}],g:{}");
        final Collection<JsonPath> keep = Arrays.asList(
            JsonPath.builder().key("d").index(3).key("e").build(),
            JsonPath.builder().key("d").index(3).key("f").build()
        );

        final JsonObject expected = parse(
            "{                        \n " +
            "  # Skipped a, b, c      \n" +
            "  d: [                   \n" +
            "    # Skipped 0 ~ 2      \n" +
            "    {                    \n" +
            "      e: {}              \n" +
            "      f: {}              \n" +
            "    }                    \n" +
            "    # Skipped 3          \n" +
            "  ]                      \n" +
            "  # Skipped g            \n" +
            "}                        \n");

        assertEquals(expected, HjsonUtils.filter(json, keep));
    }

    private static JsonObject parse(final String json) {
        return JsonValue.readHjson(json).asObject();
    }
}
