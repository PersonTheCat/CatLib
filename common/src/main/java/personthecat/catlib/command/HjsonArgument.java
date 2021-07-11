package personthecat.catlib.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.hjson.JsonObject;
import personthecat.catlib.util.Lazy;
import personthecat.catlib.util.PathTools;

import java.io.File;
import java.util.stream.Stream;

import static personthecat.catlib.exception.Exceptions.cmdEx;
import static personthecat.catlib.util.HjsonTools.readJson;
import static personthecat.catlib.util.PathTools.extension;

@SuppressWarnings("unused")
public class HjsonArgument implements ArgumentType<HjsonArgument.Result> {

    private final FileArgument getter;
    public HjsonArgument(final File dir) {
        this.getter = new FileArgument(dir);
    }

    @Override
    public Result parse(final StringReader reader) throws CommandSyntaxException {
        final File f = getter.parse(reader);
        if (f.exists() && !(f.isDirectory() || extension(f).endsWith("json"))) {
            throw cmdEx(reader, "Unsupported format");
        }
        return new Result(getter.dir, f);
    }

    public static class Result {

        private final File root;
        public final File file;
        public final Lazy<JsonObject> json;

        private Result(final File root, final File file) {
            this.root = root;
            this.file = file;
            this.json = Lazy.of(() -> {
                synchronized(this) {
                    return readJson(file).orElseGet(JsonObject::new);
                }
            });
        }

        public Stream<String> getNeighbors() {
            return PathTools.getSimpleContents(root, file);
        }
    }
}