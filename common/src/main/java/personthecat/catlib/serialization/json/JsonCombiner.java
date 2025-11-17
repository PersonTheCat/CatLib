package personthecat.catlib.serialization.json;

import personthecat.catlib.command.arguments.JsonArgument;
import xjs.data.JsonValue;

/** Used for merging JsonObject paths between json files. */
public class JsonCombiner {

    /**
     * Responsible for loading all necessary data for merging JsonObjects.
     *
     * @param from The complete path to the value being copied.
     * @param to The name of the preset being written into.
     * @param path The path of the JSON data being merged.
     */
    public static void combine(
            final JsonArgument.Result from, final JsonArgument.Result to, final JsonPath path) {
        JsonValue fromValue = path.getLastContainer(from.json.get());
        if (fromValue.isObject()) {
            final String key = path.getLast().left()
                .orElseThrow(() -> new RuntimeException("Expected an object at end of path."));
            fromValue = fromValue.asObject().get(key);
        } else if (fromValue.isArray()) {
            final int index = path.getLast().right()
                .orElseThrow(() -> new RuntimeException("Expected an array at end of path."));
            fromValue = fromValue.asArray().get(index);
        }
        path.setValue(to.json.get(), fromValue);
        XjsUtils.writeJson(to.json.get(), to.file);
    }
}