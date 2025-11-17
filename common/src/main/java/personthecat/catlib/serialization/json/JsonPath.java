package personthecat.catlib.serialization.json;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import xjs.data.JsonContainer;
import xjs.data.JsonValue;
import xjs.data.PathFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An object representing every accessor in a JSON object leading to a value.
 *
 * <p>In other words, this object is a container holding keys and indices which
 * point to a value at some arbitrary depth in a JSON array or object.
 */
public class JsonPath {
    private static final DynamicCommandExceptionType INVALID_CHARACTER =
        new DynamicCommandExceptionType(c -> Component.translatable("catlib.errorText.invalidCharacter", c));
    private static final SimpleCommandExceptionType UNEXPECTED_ACCESSOR =
        new SimpleCommandExceptionType(Component.translatable("catlib.errorText.unexpectedAccessor"));

    private final List<Either<String, Integer>> path;
    private final String raw;

    public JsonPath(final List<Either<String, Integer>> path) {
        this(path, serialize(path));
    }

    public JsonPath(final List<Either<String, Integer>> path, final String raw) {
        this.path = Collections.unmodifiableList(path);
        this.raw = raw;
    }

    /**
     * Creates a new JSON path builder, used for programmatically generating new
     * JSON path representations.
     *
     * @return A new {@link JsonPathBuilder} for constructing JSON paths.
     */
    public static JsonPathBuilder builder() {
        return new JsonPathBuilder();
    }

    /**
     * A lightweight, immutable alternative to {@link JsonPathBuilder}, specifically
     * intended for tracking paths over time in scenarios where an actual {@link JsonPath}
     * may not be needed.
     *
     * <p>For example, an application performing analysis on a body of JSON data
     * might "track" the current path using one of these objects. If for some reason
     * a specific path needs to be saved, the dev might call {@link Stub#capture()}
     * to generate a proper {@link JsonPath}, which can be reflected on at a later
     * time.
     *
     * <p>This is equivalent to using a regular {@link JsonPathBuilder}, while being
     * modestly less expensive in that context. However, because it is immutable, it
     * may be repeatedly passed into various other methods without the threat of any
     * accidental mutations further down the stack.
     *
     * @return {@link Stub#EMPTY}, for building raw JSON paths.
     */
    public static Stub stub() {
        return Stub.EMPTY;
    }

    /**
     * Deserializes the given raw path into a collection of keys and indices.
     *
     * @throws CommandSyntaxException If the path is formatted incorrectly.
     * @param raw The raw JSON path being deserialized.
     * @return An object representing every accessor leading to a JSON value.
     */
    public static JsonPath parse(final String raw) throws CommandSyntaxException {
        return parse(new StringReader(raw));
    }

    /**
     * Deserializes the given raw path into a collection of keys and indices.
     *
     * @throws CommandSyntaxException If the path is formatted incorrectly.
     * @param reader A reader exposing the raw JSON path being deserialized.
     * @return An object representing every accessor leading to a JSON value.
     */
    public static JsonPath parse(final StringReader reader) throws CommandSyntaxException {
        final List<Either<String, Integer>> path = new ArrayList<>();
        final int begin = reader.getCursor();

        while (reader.canRead() && reader.peek() != ' ') {
            final char c = reader.read();
            if (c == '.') {
                checkDot(reader, begin);
            } else if (inKey(c)) {
                path.add(Either.left(c + readKey(reader)));
            } else if (c == '[') {
                checkDot(reader, begin);
                path.add(Either.right(reader.readInt()));
                reader.expect(']');
            } else {
                throw INVALID_CHARACTER.createWithContext(reader, c);
            }
        }
        return new JsonPath(path, reader.getString().substring(begin, reader.getCursor()));
    }

    /**
     * Variant of {@link #parse(String)} which returns instead of throwing
     * an exception.
     *
     * @param raw The raw JSON path being deserialized.
     * @return An object representing every accessor leading to a JSON value.
     */
    public static DataResult<JsonPath> tryParse(final String raw) {
        try {
            return DataResult.success(parse(raw));
        } catch (final CommandSyntaxException e) {
            return DataResult.error(e::getMessage);
        }
    }

    private static String readKey(final StringReader reader) {
        final int start = reader.getCursor();
        while (reader.canRead() && inKey(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    private static boolean inKey(final char c) {
        return c != '.' && c != ' ' && c != '[';
    }

    private static void checkDot(final StringReader reader, final int begin) throws CommandSyntaxException {
        final int cursor = reader.getCursor();
        if (cursor < 2) {
            return;
        }
        final char last = reader.getString().charAt(cursor - 2);
        if (cursor - 1 == begin || last == '.') {
            throw UNEXPECTED_ACCESSOR.createWithContext(reader);
        }
    }

    /**
     * Converts the given JSON path data into a raw string.
     *
     * @param path The parsed JSON path being serialized.
     * @return A string representing the equivalent path.
     */
    public static String serialize(final Collection<Either<String, Integer>> path) {
        final StringBuilder sb = new StringBuilder();
        for (final Either<String, Integer> either : path) {
            either.ifLeft(s -> {
                sb.append('.');
                sb.append(s);
            });
            either.ifRight(i -> {
                sb.append('[');
                sb.append(i);
                sb.append(']');
            });
        }
        final String s = sb.toString();
        return s.startsWith(".") ? s.substring(1) : s;
    }

    /**
     * Generates a list of every possible JSON path in this object.
     *
     * @param json The json containing the expected paths.
     * @return A list of objects representing these paths.
     */
    public static List<JsonPath> getAllPaths(final JsonContainer json) {
        return toPaths(json.getPaths());
    }

    /**
     * Generates a list of every used JSON path in this object.
     *
     * @param json The json containing the expected paths.
     * @return A list of objects representing these paths.
     */
    public static List<JsonPath> getUsedPaths(final JsonContainer json) {
        return toPaths(json.getPaths(PathFilter.USED));
    }

    /**
     * Generates a list of every unused JSON path in this object.
     *
     * @param json The json containing the expected paths.
     * @return A list of objects representing these paths.
     */
    public static List<JsonPath> getUnusedPaths(final JsonContainer json) {
        return toPaths(json.getPaths(PathFilter.UNUSED));
    }

    private static List<JsonPath> toPaths(final List<String> raw) {
        return raw.stream()
            .map(JsonPath::parseUnchecked)
            .collect(Collectors.toList());
    }

    private static JsonPath parseUnchecked(final String path) {
        try {
            return parse(path);
        } catch (final CommandSyntaxException e) {
            throw new IllegalStateException("JSON lib returned unusable path", e);
        }
    }

    public JsonContainer getLastContainer(final JsonContainer json) {
        return XjsUtils.getLastContainer(json, this.path);
    }

    public Optional<JsonValue> getValue(final JsonContainer json) {
        return XjsUtils.getValueFromPath(json, this.path);
    }

    public void setValue(final JsonContainer json, final @Nullable JsonValue value) {
        XjsUtils.setValueFromPath(json, this.path, value);
    }

    public JsonPath getClosestMatch(final JsonContainer json) {
        return XjsUtils.getClosestMatch(json, this.path);
    }

    public int getLastAvailable(final JsonContainer json) {
        return XjsUtils.getLastAvailable(json, this.path);
    }

    public JsonPathBuilder toBuilder() {
        return new JsonPathBuilder(new ArrayList<>(this.path), new StringBuilder(this.raw));
    }

    public Stub beginTracking() {
        return new Stub(this.raw);
    }

    public List<Either<String, Integer>> asList() {
        return this.path;
    }

    public String asRawPath() {
        return this.raw;
    }

    public Either<String, Integer> getLast() {
        return this.path.getLast();
    }

    public boolean isEmpty() {
        return this.path.isEmpty();
    }

    public int size() {
        return this.path.size();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof JsonPath) {
            return this.path.equals(((JsonPath) o).path);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.path.hashCode();
    }

    @Override
    public String toString() {
        return this.raw;
    }

    /**
     * A builder used for manually constructing JSON paths in-code.
     */
    public static class JsonPathBuilder {

        private final List<Either<String, Integer>> path;
        private final StringBuilder raw;

        private JsonPathBuilder() {
            this(new ArrayList<>(), new StringBuilder());
        }

        private JsonPathBuilder(final List<Either<String, Integer>> path, final StringBuilder raw) {
            this.path = path;
            this.raw = raw;
        }

        public JsonPathBuilder key(final String key) {
            this.path.add(Either.left(key));
            if (!this.raw.isEmpty()) {
                this.raw.append('.');
            }
            this.raw.append(key);
            return this;
        }

        public JsonPathBuilder index(final int index) {
            this.path.add(Either.right(index));
            this.raw.append('[').append(index).append(']');
            return this;
        }

        public JsonPathBuilder up(final int count) {
            JsonPathBuilder builder = this;
            for (int i = 0; i < count; i++) {
                builder = builder.up();
            }
            return builder;
        }

        public JsonPathBuilder up() {
            if (this.path.isEmpty()) {
                return this;
            } else if (this.path.size() == 1) {
                return new JsonPathBuilder();
            }
            this.path.removeLast();
            final int dot = this.raw.lastIndexOf(".");
            final int bracket = this.raw.lastIndexOf("[");
            this.raw.delete(Math.max(dot, bracket), this.raw.length());
            return this;
        }

        public JsonPathBuilder append(final JsonPath path, final int startInclusive) {
            return this.append(path.path, startInclusive);
        }

        public JsonPathBuilder append(
                final List<Either<String, Integer>> path, final int startInclusive) {
            return this.append(path, startInclusive, path.size());
        }

        public JsonPathBuilder append(final JsonPath path, final int startInclusive, final int endExclusive) {
            return this.append(path.path, startInclusive, endExclusive);
        }

        public JsonPathBuilder append(
                final List<Either<String, Integer>> path, final int startInclusive, final int endExclusive) {
            for (int i = startInclusive; i < endExclusive; i++) {
                path.get(i).ifLeft(this::key).ifRight(this::index);
            }
            return this;
        }

        public JsonPath build() {
            return new JsonPath(this.path, this.raw.toString());
        }

        @Override
        public int hashCode() {
            return this.path.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof JsonPathBuilder) {
                return this.path.equals(((JsonPathBuilder) o).path);
            }
            return false;
        }
    }

    /**
     * A lightweight, immutable builder designed for appending paths over time, wherein
     * the more expensive {@link JsonPath} is typically unneeded. 
     */
    public static class Stub {

        private static final Stub EMPTY = new Stub("");

        private final String path;

        private Stub(final String path) {
            this.path = path;
        }

        public Stub key(final String key) {
            if (this.path.isEmpty()) {
                return new Stub(key);
            }
            return new Stub(this.path + "." + key);
        }

        public Stub index(final int index) {
            return new Stub(this.path + "[" + index + "]");
        }

        public JsonPath capture() {
            try {
                return parse(this.path);
            } catch (final CommandSyntaxException e) {
                throw new IllegalArgumentException("Invalid characters in stub: " + this.path);
            }
        }

        @Override
        public int hashCode() {
            return this.path.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof Stub) {
                return this.path.equals(((Stub) o).path);
            }
            return false;
        }
    }
}
