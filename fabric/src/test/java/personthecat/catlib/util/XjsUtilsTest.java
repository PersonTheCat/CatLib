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
        final JsonObject json = parse("""
            {
              a: {}
              b: {}
              c: {}
              d: [
                {}
                {}
                {}
                {
                  e: {}
                  f: {}
                }
                {}
              ]
              g: {}
            }
            """);

        final Collection<JsonPath> keep = Arrays.asList(
            JsonPath.builder().key("d").index(3).key("e").build(),
            JsonPath.builder().key("d").index(3).key("f").build()
        );

        final JsonObject expected = parse("""
            {
              // Skipped a, b, c
              d: [
                // Skipped 0 ~ 2
                {
                  e: {}
                  f: {}
                }
                // Skipped 4
              ]
              // Skipped g
            }
            """);

        assertEquals(expected, XjsUtils.filter(json, keep));
    }

    @Test
    public void filter_onObjectWithComments_doesNotMangleComments() {
        final JsonObject json = parse("""
            {
              a: {}
              b: {}
              c: {}
              d: [
                {}
                {}
                {}
                // Generated value
                {
                  e: {}
                  f: {}
                }
                {}
              ]
              g: {}
            }
            """);

        final Collection<JsonPath> keep = Arrays.asList(
            JsonPath.builder().key("d").index(3).key("e").build(),
            JsonPath.builder().key("d").index(3).key("f").build()
        );

        final JsonObject expected = parse("""
            {
              // Skipped a, b, c
              d: [
                // Skipped 0 ~ 2
                // Generated value
                {
                  e: {}
                  f: {}
                }
                // Skipped 4
              ]
              // Skipped g
            }
            """);

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
