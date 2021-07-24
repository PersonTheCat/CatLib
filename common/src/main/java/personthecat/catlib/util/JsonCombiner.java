package personthecat.catlib.util;

import org.hjson.JsonValue;
import personthecat.catlib.command.arguments.HjsonArgument;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.io.FileIO;

import java.io.File;

import static personthecat.catlib.exception.Exceptions.runEx;

/** Used for merging JsonObject paths between json files. */
public class JsonCombiner {

    /**
     * Responsible for loading all necessary data for merging JsonObjects.
     *
     * @param from The complete path to the value being copied.
     * @param to The name of the preset being written into.
     * @param path The path of the JSON data being merged.
     * @param backups The directory where backups are stored for this mod.
     * @return The number of times a backup was created.
     */
    public static int combine(final HjsonArgument.Result from, final HjsonArgument.Result to,
                               final PathArgument.Result path, final File backups) {
        JsonValue fromValue = HjsonTools.getLastContainer(from.json.get(), path);
        if (fromValue.isObject()) {
            final String key = path.path.get(path.path.size() - 1).left()
                .orElseThrow(() -> runEx("Expected an object at end of path."));
            fromValue = fromValue.asObject().get(key);
        } else if (fromValue.isArray()) {
            final int index = path.path.get(path.path.size() - 1).right()
                .orElseThrow(() -> runEx("Expected an array at end of path."));
            fromValue = fromValue.asArray().get(index);
        }
        HjsonTools.setValueFromPath(to.json.get(), path, fromValue);
        final int count = FileIO.backup(backups, to.file, true);
        HjsonTools.writeJson(to.json.get(), to.file);
        return count;
    }
}