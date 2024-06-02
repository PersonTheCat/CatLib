package personthecat.catlib.config.neo;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FormatDetector;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.io.WritingException;
import org.jetbrains.annotations.Nullable;
import xjs.data.Json;
import xjs.data.JsonFormat;
import xjs.data.JsonObject;
import xjs.data.JsonValue;
import xjs.data.comments.CommentType;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.parser.DjsParser;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DjsConfigFormat implements ConfigFormat<CommentedConfig> {
    public static final DjsConfigFormat INSTANCE = new DjsConfigFormat();

    private DjsConfigFormat() {}

    @Override
    public ConfigWriter createWriter() {
        return new Writer();
    }

    @Override
    public ConfigParser<CommentedConfig> createParser() {
        return new Parser();
    }

    @Override
    public CommentedConfig createConfig(Supplier<Map<String, Object>> mapCreator) {
        return CommentedConfig.of(mapCreator, this);
    }

    @Override
    public boolean supportsComments() {
        return true;
    }

    public static void registerFileFormat() {
        FormatDetector.registerExtension("djs", INSTANCE);
    }

    private static class Writer implements ConfigWriter {

        @Override
        public void write(UnmodifiableConfig config, java.io.Writer tw) {
            try {
                toXjs(config).write(tw);
            } catch (final IOException e) {
                throw new WritingException(e);
            }
        }

        @Override
        public String writeToString(UnmodifiableConfig config) {
            return toXjs(config).toString(JsonFormat.DJS_FORMATTED);
        }

        private static JsonValue toXjs(Object o) {
            return o instanceof UnmodifiableConfig c ? toXjs(c) : Json.any(o);
        }

        private static JsonObject toXjs(UnmodifiableConfig config) {
            final JsonObject json = new JsonObject();
            getCommentedMap(config).forEach((key, value) -> {
                final JsonValue v = toXjs(value.value);
                if (value.comment != null) {
                    v.setComment(value.comment);
                }
                json.set(key, v);
            });
            return json;
        }

        private static CommentedValueMap getCommentedMap(UnmodifiableConfig config) {
            final CommentedValueMap map = new CommentedValueMap();
            final Map<String, String> comments = config instanceof CommentedConfig c ? c.commentMap() : Map.of();
            config.valueMap().forEach((key, value) ->
                map.put(key, new CommentedValue(toXjs(value), comments.get(key))));
            return map;
        }
    }

    private static class Parser implements ConfigParser<CommentedConfig> {

        @Override
        public ConfigFormat<CommentedConfig> getFormat() {
            return DjsConfigFormat.INSTANCE;
        }

        @Override
        public CommentedConfig parse(Reader reader) {
            final CommentedConfig config = DjsConfigFormat.INSTANCE.createConfig();
            this.parse(reader, config, ParsingMode.MERGE);
            return config;
        }

        @Override
        public void parse(Reader reader, Config config, ParsingMode mode) {
            try {
                this.parse(PositionTrackingReader.fromReader(reader), config, mode);
            } catch (final IOException e) {
                throw new ParsingException(e.getMessage(), e);
            }
        }

        @Override
        public CommentedConfig parse(String input) {
            final CommentedConfig config = DjsConfigFormat.INSTANCE.createConfig();
            try {
                this.parse(PositionTrackingReader.fromString(input), config, ParsingMode.MERGE);
            } catch (final IOException e) {
                throw new ParsingException(e.getMessage(), e);
            }
            return config;
        }

        @Override
        public void parse(String input, Config config, ParsingMode mode) {
            try {
                this.parse(PositionTrackingReader.fromString(input), config, mode);
            } catch (final IOException e) {
                throw new ParsingException(e.getMessage(), e);
            }
        }

        public void parse(PositionTrackingReader reader, Config config, ParsingMode mode) throws IOException {
            this.toFlatMap(this.parseAsJson(reader)).forEach((path, value) -> {
                if (value.value != null) {
                    mode.put(config, path, value.value);
                }
                if (value.comment != null && config instanceof CommentedConfig c) {
                    c.setComment(path, value.comment);
                }
            });
        }

        public JsonObject parseAsJson(PositionTrackingReader reader) throws IOException {
            final JsonValue v;
            try {
                v = new DjsParser(reader).parse();
            } catch (final SyntaxException e) {
                throw new ParsingException(e.getMessage(), e);
            }
            if (!v.isObject()) {
                throw new ParsingException("Expected an object: " + v);
            }
            return v.asObject();
        }

        private FlatCommentedMap toFlatMap(final JsonObject o) {
            final FlatCommentedMap map = new FlatCommentedMap();
            this.toFlatMap(map, List.of(), o);
            return map;
        }

        private void toFlatMap(final FlatCommentedMap map, final List<String> path, final JsonObject o) {
            for (final JsonObject.Member m : o) {
                final List<String> fullPath = concat(path, m.getKey());
                final JsonValue value = m.getValue();
                final String comment =
                    value.hasComment(CommentType.HEADER) ? value.getComment(CommentType.HEADER) : null;

                if (value.isObject()) {
                    if (comment != null) {
                        map.put(fullPath, new OptionalCommentedValue(null, comment));
                    }
                    this.toFlatMap(map, fullPath, value.asObject());
                } else {
                    map.put(fullPath, new OptionalCommentedValue(value.unwrap(), comment));
                }
            }
        }

        private static List<String> concat(final List<String> path, final String s) {
            final List<String> l = new ArrayList<>(path);
            l.add(s);
            return l;
        }
    }

    private static class CommentedValueMap extends HashMap<String, CommentedValue> {}
    private record CommentedValue(Object value, @Nullable String comment) {}
    private static class FlatCommentedMap extends HashMap<List<String>, OptionalCommentedValue> {}
    private record OptionalCommentedValue(@Nullable Object value, @Nullable String comment) {}
}
