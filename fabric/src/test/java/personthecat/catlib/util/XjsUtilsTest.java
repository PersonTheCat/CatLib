package personthecat.catlib.util;

import org.junit.jupiter.api.Test;
import personthecat.catlib.serialization.json.XjsUtils;
import personthecat.catlib.serialization.json.JsonPath;
import xjs.data.Json;
import xjs.data.JsonObject;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class XjsUtilsTest {

    @Test
    public void filter_generatesObject_withGivenPathsOnly() {
        final JsonObject json = parse(
            "{                        \n" +
            "  a: {}                  \n" +
            "  b: {}                  \n" +
            "  c: {}                  \n" +
            "  d: [                   \n" +
            "    {}                   \n" +
            "    {}                   \n" +
            "    {}                   \n" +
            "    {                    \n" +
            "      e: {}              \n" +
            "      f: {}              \n" +
            "    }                    \n" +
            "    {}                   \n" +
            "  ]                      \n" +
            "  g: {}                  \n" +
            "}                        \n");

        final Collection<JsonPath> keep = Arrays.asList(
            JsonPath.builder().key("d").index(3).key("e").build(),
            JsonPath.builder().key("d").index(3).key("f").build()
        );

        final JsonObject expected = parse(
            "{                        \n " +
            "  // Skipped a, b, c     \n" +
            "  d: [                   \n" +
            "    // Skipped 0 ~ 2     \n" +
            "    {                    \n" +
            "      e: {}              \n" +
            "      f: {}              \n" +
            "    }                    \n" +
            "    // Skipped 4         \n" +
            "  ]                      \n" +
            "  // Skipped g           \n" +
            "}                        \n");

        assertEquals(expected, XjsUtils.filter(json, keep));
    }

    @Test
    public void filter_onObjectWithComments_doesNotMangleComments() {
        final JsonObject json = parse(
            "{                        \n" +
            "  a: {}                  \n" +
            "  b: {}                  \n" +
            "  c: {}                  \n" +
            "  d: [                   \n" +
            "    {}                   \n" +
            "    {}                   \n" +
            "    {}                   \n" +
            "    // Generated value   \n" +
            "    {                    \n" +
            "      e: {}              \n" +
            "      f: {}              \n" +
            "    }                    \n" +
            "    {}                   \n" +
            "  ]                      \n" +
            "  g: {}                  \n" +
            "}                        \n");

        final Collection<JsonPath> keep = Arrays.asList(
            JsonPath.builder().key("d").index(3).key("e").build(),
            JsonPath.builder().key("d").index(3).key("f").build()
        );

        final JsonObject expected = parse(
            "{                        \n " +
            "  // Skipped a, b, c     \n" +
            "  d: [                   \n" +
            "    // Skipped 0 ~ 2     \n" +
            "    // Generated value   \n" +
            "    {                    \n" +
            "      e: {}              \n" +
            "      f: {}              \n" +
            "    }                    \n" +
            "    // Skipped 4         \n" +
            "  ]                      \n" +
            "  // Skipped g           \n" +
            "}                        \n");

        assertEquals(expected, XjsUtils.filter(json, keep));
    }

    private static JsonObject parse(final String json) {
        return Json.parse(trimLines(json)).asObject();
    }

    private static String trimLines(final String text) {
        // The XJS library might store extra spaces in comments
        // This prevents that from affecting our test cases.
        return text.replaceAll("\\s*\n", "\n");
    }
}
