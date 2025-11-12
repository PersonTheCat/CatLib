package personthecat.catlib.serialization.json;

import com.mojang.datafixers.util.Either;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.command.arguments.PathArgument;
import xjs.data.comments.CommentType;
import xjs.data.*;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.writer.JsonWriterOptions;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;

import static java.util.Optional.empty;
import static personthecat.catlib.util.LibUtil.f;

/**
 * A collection of convenience methods for interacting with XJS objects. Unlike
 * the original methods inside of {@link JsonObject}, most of the utilities in this
 * class return values wrapped in {@link Optional}, instead of <code>null</code>.
 * <p>
 *   In a future version of this library (via Exjson/xjs-core), JSON objects will
 *   support returning {@link Optional} out of the box, as well as the options to
 *   flatten arrays, support additional data types, and more. As a result, most
 *   of these utilities will eventually be deprecated.
 * </p>
 */
@Log4j2
public final class XjsUtils {

    private XjsUtils() {}

    /**
     * Reads a {@link JsonObject} from the given file.
     *
     * @param file The file containing the serialized JSON object.
     * @return The deserialized object, or else {@link Optional#empty}.
     */
    public static Optional<JsonObject> readJson(final Path file) {
        try {
            return Optional.of(Json.parse(file).asObject());
        } catch (final IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                log.warn("Error parsing JSON file", e);
            }
        }
        return Optional.empty();
    }

    /**
     * Variant of {@link #readJson(Path)} which ignores syntax errors
     * and simply returns {@link Optional#empty} if any error occurs.
     *
     * @param file The file containing the serialized JSON object.
     * @return The deserialized object, or else {@link Optional#empty}.
     */
    public static Optional<JsonObject> readSuppressing(final Path file) {
        try {
            return readJson(file);
        } catch (final SyntaxException e) {
            log.warn("Suppressing syntax exception in file: {}", file.toAbsolutePath(), e);
            return Optional.empty();
        }
    }

    /**
     * Writes a regular {@link JsonObject} to the disk. The format of this output file
     * is automatically determined by its extension.
     * <p>
     *   Any file extended with <code>.json</code> will be written in regular JSON
     *   format. All other extensions will implicitly be treated as XJS.
     * </p>
     * <p>
     *   No {@link IOException}s will be thrown by this method. Instead, they will be
     *   logged and simply returned for the caller to optionally throw.
     * </p>
     * <p>
     *   All other exceptions <b>will be thrown</b> by this method.
     * </p>
     * @param json The JSON data being serialized.
     * @param file The destination file containing these data.
     */
    public static void writeJson(final JsonObject json, final Path file) {
        try {
            JsonContext.autoWrite(file, json);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads a file from the disk <em>and</em> updates it.
     * <p>
     *   For example,
     * </p>
     * <pre>{@code
     *   XJSTools.updateJson(file, json -> {
     *      json.set("hello", "world");
     *   });
     * }</pre>
     * <p>
     *   The output of this expression will be applied to the original file.
     * </p>
     * @param file the file containing JSON data.
     * @param f    Instructions for updating the JSON data.
     */
    public static void updateJson(final Path file, final UnaryOperator<JsonObject> f) {
        // If #readJson returned empty, it's because the file didn't exist.
        final var json = readJson(file).orElseGet(JsonObject::new);
        writeJson(f.apply(json), file);
    }

    /**
     * Gets the default formatting options, guaranteed to never print a `\r` character,
     * which Minecraft does not print correctly in-game.
     *
     * @return The default formatting options without <code>\r</code>.
     */
    public static JsonWriterOptions noCr() {
        return JsonContext.getDefaultFormatting().setEol("\n");
    }

    /**
     * Updates a single value in a JSON object based on a full, dotted path.
     * <p>
     *   For example,
     * </p>
     * <pre>
     *   /update my_json path.to.field true
     * </pre>
     * @param json The JSON object or array containing this path.
     * @param path The output of a {@link PathArgument}.
     * @param value The updated value to set at this path.
     */
    public static void setValueFromPath(final JsonContainer json, final JsonPath path, @Nullable final JsonValue value) {
        if (path.isEmpty()) {
            return;
        }
        final Either<String, Integer> lastVal = path.get(path.size() - 1);
        final JsonContainer parent = getLastContainer(json, path);
        // This will ideally be handled by XJS in the future.
        if (value != null && value.getLinesAbove() == -1 && condenseNewValue(path, parent)) {
            value.setLinesAbove(0);
        }
        setEither(parent, lastVal, value);
    }

    /**
     * Determines whether to format an incoming value as condensed.
     *
     * @param path      The path to the value being set.
     * @param container The parent container for this new value.
     * @return <code>true</code>, if the value should be condensed.
     */
    private static boolean condenseNewValue(final JsonPath path, final JsonContainer container) {
        if (container.isEmpty()) {
            return true;
        }
        final int s = path.size() == 1 && container.isObject() ? 1 : 0;
        for (int i = s; i < container.size(); i++) {
            if (container.getReference(i).getOnly().getLinesAbove() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a single value in a JSON object based on a full, dotted path.
     *
     * @param json The JSON object or array containing this path.
     * @param path The output of a {@link PathArgument}.
     * @return The value at this location, or else {@link Optional#empty}.
     */
    public static Optional<JsonValue> getValueFromPath(final JsonContainer json, final JsonPath path) {
        if (path.isEmpty()) {
            return empty();
        }
        final Either<String, Integer> lastVal = path.get(path.size() - 1);
        return getEither(getLastContainer(json, path), lastVal);
    }

    /**
     * Retrieves the last JsonObject or JsonArray represented by this path.
     * <p>
     *   For example, a path of
     * </p>
     * <pre>
     *   object1.array2.object3.value4
     * </pre>
     * <p>
     *   will return <code>object3</code> when passed into this method.
     * </p>
     * <p>
     *   If no object or array exists at this location, a new container will be created at this
     *   location and returned by the method.
     * </p>
     * @param json The JSON object or array containing this path.
     * @param path The output of a {@link PathArgument}.
     * @return The value at this location, the original <code>json</code>, or else a new container.
     */
    public static JsonContainer getLastContainer(final JsonContainer json, final JsonPath path) {
        if (path.isEmpty()) {
            return json;
        }
        JsonContainer current = json;
        for (int i = 0; i < path.size() - 1; i++) {
            final Either<String, Integer> val = path.get(i);
            final Either<String, Integer> peek = path.get(i + 1);

            if (val.right().isPresent()) { // Index
                current = getOrTryNew(current.asArray(), val.right().get(), peek);
            } else if (peek.left().isPresent()) { // Key -> key -> object
                current = getOrCreateObject(current.asObject(), val.orThrow());
            } else { // Key -> index -> array
                current = getOrCreateArray(current.asObject(), val.orThrow());
            }
        }
        return current;
    }

    /**
     * Gets the index of the last available element in this path, or else -1.
     *
     * <p>For example, when given the following JSON object:</p>
     * <pre>
     *   a:{b:[]}
     * </pre>
     * <p>And the following path:</p>
     * <pre>
     *   a.b[0].c
     * </pre>
     * <p>An index of 1 (pointing to b) will be returned.</p>
     *
     * @param json The JSON object or array containing the data being inspected.
     * @param path The path to the expected data, which may or may not exist.
     * @return The index to the last matching element, or else -1.
     */
    public static int getLastAvailable(final JsonContainer json, final JsonPath path) {
        final MutableObject<JsonValue> current = new MutableObject<>(json);
        int index = -1;

        for (final Either<String, Integer> component : path) {
            component.ifLeft(key -> {
                final JsonValue value = current.getValue();
                if (value.isObject()) {
                    current.setValue(value.asObject().get(key));
                } else {
                    current.setValue(null);
                }
            }).ifRight(i -> {
                final JsonValue value = current.getValue();
                if (value.isArray() && i < value.asArray().size()) {
                    current.setValue(value.asArray().get(i));
                } else {
                    current.setValue(null);
                }
            });
            if (current.getValue() == null) {
                return index;
            }
            index++;
        }
        return index;
    }

    /**
     * Attempts to resolve the closest matching path in the given JSON data.
     *
     * <p>Essentially, this method accepts the canonicalized path of an expected value for the
     * data being represented. It will account for the possibility that <b>object arrays may be
     * expressed as singletons</b> and return the <em>actual</em> path, should any be used.</p>
     *
     * @param json The object or array being inspected.
     * @param path The canonicalized path to the expected value
     * @return The actual path to the value, or else the canonical path.
     */
    public static JsonPath getClosestMatch(final JsonContainer json, final JsonPath path) {
        final MutableObject<JsonValue> current = new MutableObject<>(json);
        final JsonPath.JsonPathBuilder builder = JsonPath.builder();

        for (int i = 0; i < path.size(); i++) {
            path.get(i).ifLeft(key -> {
                JsonValue value = current.getValue();
                while (value.isArray() && !value.asArray().isEmpty()) {
                    builder.index(0);
                    value = value.asArray().get(0);
                }
                if (value.isObject() && value.asObject().has(key)) {
                    current.setValue(value.asObject().get(key));
                    builder.key(key);
                } else {
                    current.setValue(null);
                }
            }).ifRight(index -> {
                final JsonValue value = current.getValue();
                if (value.isArray() && value.asArray().size() > index) {
                    current.setValue(value.asArray().get(index));
                    builder.index(index);
                } else if (!(value.isObject() && index == 0)) {
                    current.setValue(null);
                }
            });
            if (current.getValue() == null) {
                return builder.build().append(path, i);
            }
        }
        return builder.build();
    }

    /**
     * Filters values from the given JSON object according to a list of expected paths.
     *
     * @param json  The JSON object and source being transformed.
     * @param paths The paths expected to stay in the output.
     * @return A transformed object containing only the expected paths.
     */
    public static JsonObject filter(final JsonObject json, final Collection<JsonPath> paths) {
        return filter(json, paths, false);
    }

    /**
     * Filters values from the given JSON object according to a list of expected paths.
     *
     * @param json      The JSON object and source being transformed.
     * @param paths     The paths expected to stay in the output.
     * @param blacklist Whether to optionally blacklist these paths.
     * @return A transformed object containing only the expected paths.
     */
    public static JsonObject filter(final JsonObject json, final Collection<JsonPath> paths, final boolean blacklist) {
        final JsonObject clone = (JsonObject) json.deepCopy();
        // Flag each path as used so anything else will get removed.
        paths.forEach(path -> path.getValue(clone));
        return skip(clone, blacklist);
    }

    /**
     * Generates a new {@link JsonObject} containing only the values that were or were not
     * used in the original.
     *
     * @param json The original JSON object being transformed.
     * @param used <code>true</code> to skip used values, <code>false</code> to skip unused.
     * @return A <b>new</b> JSON object with these values trimmed out.
     */
    public static JsonObject skip(final JsonObject json, final boolean used) {
        final JsonObject generated = (JsonObject) new JsonObject().setDefaultMetadata(json);
        final StringBuilder skipped = new StringBuilder();

        for (final JsonObject.Member member : json) {
            final JsonValue value = member.getOnly();
            final String name = member.getKey();

            if (member.getReference().isAccessed() != used) {
                if (!skipped.isEmpty()) {
                    value.prependComment("Skipped " + skipped);
                    skipped.setLength(0);
                }
                if (value.isObject()) {
                    generated.add(name, skip(value.asObject(), used));
                } else if (value.isArray()) {
                    generated.add(name, skip(value.asArray(), used));
                } else {
                    generated.add(name, value);
                }
            } else if (skipped.isEmpty()) {
                skipped.append(name);
            } else {
                skipped.append(", ").append(name);
            }
        }
        if (!skipped.isEmpty()) {
            generated.prependComment(CommentType.INTERIOR, "Skipped " + skipped);
        }
        return generated;
    }

    /**
     * Generates a new {@link JsonArray} containing only the values that were or were not
     * used in the original.
     *
     * @param json The original JSON array being transformed.
     * @param used <code>true</code> to skip used values, <code>false</code> to skip unused.
     * @return A <b>new</b> JSON array with these values trimmed out.
     */
    public static JsonArray skip(final JsonArray json, final boolean used) {
        final JsonArray generated = (JsonArray) new JsonArray().setDefaultMetadata(json);
        int lastIndex = 0;
        int index = 0;

        for (final JsonReference reference : json.references()) {
            final JsonValue value = reference.getOnly();
            if (reference.isAccessed() != used) {
                if (index == lastIndex + 1) {
                    value.prependComment("Skipped " + (index - 1));
                } else if (index > lastIndex) {
                    value.prependComment("Skipped " + lastIndex + " ~ " + (index - 1));
                }
                if (value.isObject()) {
                    generated.add(skip(value.asObject(), used));
                } else if (value.isArray()) {
                    generated.add(skip(value.asArray(), used));
                } else {
                    generated.add(value);
                }
                lastIndex = index + 1;
            }
            index++;
        }
        if (index == lastIndex + 1) {
            generated.prependComment(CommentType.INTERIOR, "Skipped " + (index - 1));
        } else if (index > lastIndex) {
            generated.prependComment(CommentType.INTERIOR, "Skipped " + lastIndex + " ~ " + (index - 1));
        }
        return generated;
    }

    /**
     * Retrieves a list of paths adjacent to the input path. This can be used to provide
     * command suggestions as the user is walking through this container.
     * <p>
     *   For example, when given the following JSON object:
     * </p>
     * <pre>
     *   a: [
     *     {
     *       b: { b1: true }
     *       c: { c1: false }
     *     }
     *   ]
     * </pre>
     * <p>
     *   and the following <b>incomplete</b> command:
     * </p>
     * <pre>
     *   /update my_json a[0]
     * </pre>
     * <p>
     *   the following paths will be returned:
     * </p>
     * <ul>
     *   <li>a[0].b</li>
     *   <li>a[0].c</li>
     * </ul>
     * @param json The JSON data containing these paths.
     * @param path The current output of a {@link PathArgument}.
     * @return A list of all adjacent paths.
     */
    public static List<String> getPaths(final JsonObject json, final JsonPath path) {
        JsonValue container;
        try {
            container = getLastContainer(json, path);
        } catch (final Exception e) {
            log.warn("Error getting container value", e);
            container = json;
        }
        int end = path.size() - 1;
        if (end < 0) {
            return getNeighbors("", container);
        }
        final Optional<JsonValue> v = getEither(container, path.get(end))
            .filter(value -> value.isObject() || value.isArray());
        if (v.isPresent()) {
            end++; // The full path is a valid container -> use it.
        }
        final String dir = JsonPath.serialize(path.subList(0, end));
        return getNeighbors(dir, v.orElse(container));
    }

    /**
     * Retrieves a list of paths in the given container.
     *
     * @param dir The path to this container, as a string.
     * @param container The {@link JsonObject} or {@link JsonArray} at this location.
     * @return A formatted list of all members at this location.
     */
    private static List<String> getNeighbors(final String dir, final JsonValue container) {
        final List<String> neighbors = new ArrayList<>();
        if (container.isObject()) {
            for (JsonObject.Member member : container.asObject()) {
                final String name = member.getKey();
                neighbors.add(dir.isEmpty() ? name : f("{}.{}", dir, name));
            }
        } else if (container.isArray()) {
            for (int i = 0; i < container.asArray().size(); i++) {
                neighbors.add(f("{}[{}]", dir, i));
            }
        }
        return neighbors;
    }

    /**
     * Attempts to retrieve an object or an array. Creates a new one, if absent.
     *
     * @throws IndexOutOfBoundsException If index > array.size()
     * @param array The JSON array containing the researched data.
     * @param index The index of the data in the array.
     * @param type The path element at this index, indicating either a key or an index.
     * @return Either a JSON object or array, whichever is at this location.
     */
    private static JsonContainer getOrTryNew(final JsonArray array, final int index, final Either<String, Integer> type) {
        if (index == array.size()) { // The value must be added.
            type.ifLeft(s -> array.add(new JsonObject()))
                .ifRight(i -> array.add(new JsonArray()));
        } // if index >= newSize -> index out of bounds
        return array.get(index).asContainer();
    }

    /**
     * Attempts to retrieve either an object or an array from a JSON container.
     * <p>
     *   If this value is a string, it will be treated as a key. If the value is a
     *   number, it will be treated as an index.
     * </p>
     * @param container Either a JSON object or array
     * @param either The accessor for the value at this location.
     */
    private static Optional<JsonValue> getEither(final JsonValue container, final Either<String, Integer> either) {
        return either.map(
            s -> Optional.ofNullable(container.asObject().get(s)),
            i -> i < container.asArray().size() ? Optional.of(container.asArray().get(i)) : empty());
    }

    /**
     * Attempts to set a value in a container which may either be an object or an array.
     *
     * @param container Either a JSON object or array.
     * @param either The accessor for this value, either a key or an index.
     * @param value The value to set at this location.
     */
    private static void setEither(final JsonValue container, final Either<String, Integer> either, @Nullable final JsonValue value) {
        if (either.left().isPresent()) {
            if (value == null) {
                container.asObject().remove(either.left().get());
            } else if (value.hasComments()) {
                container.asObject().set(either.left().get(), value);
            } else {
                final String key = either.left().get();
                final JsonObject object = container.asObject();
                object.set(key, value);
            }
        } else if (either.right().isPresent()) { // Just to stop the linting.
            if (value == null) {
                container.asArray().remove(either.right().get());
            } else if (value.hasComments()) {
                container.asArray().set(either.right().get(), value);
            } else {
                final int index = either.right().get();
                final JsonArray array = container.asArray();
                setOrAdd(array, index, value);
            }
        }
    }

    /**
     * Sets the value at the given index, or else if <code>index == array.size()</code>, adds it.
     *
     * @param array The array being added into.
     * @param index The index of the value being set.
     * @param value The value being set.
     * @return <code>array</code>, for method chaining.
     * @throws IndexOutOfBoundsException If <code>index &lt; 0 || index &gt; size</code>
     */
    public static JsonArray setOrAdd(final JsonArray array, final int index, final JsonValue value) {
        if (index == array.size()) {
            return array.add(value);
        }
        return array.set(index, value);
    }

    /**
     * Gets a list of any objects for the given key.
     * <p>
     *   Note that any non-object values in this array will <b>not be returned</b>.
     * </p>
     * <p>
     *   For example, when given the following JSON array:
     * </p>
     * <pre>
     *   array: [{},{},true,[[{}]]]
     * </pre>
     * <p>
     *   This array will be returned:
     * </p>
     * <pre>
     *   [{},{},{}]
     * </pre>
     * @param json The JSON object containing the array.
     * @param field The key where this array is stored.
     * @return A list of all {@link JsonObject}s at this location.
     */
    public static List<JsonObject> getRegularObjects(final JsonObject json, final String field) {
        final List<JsonObject> list = new ArrayList<>();
        final JsonArray array = json.getOptional(field)
            .map(JsonValue::intoArray)
            .orElseGet(JsonArray::new);
        flattenRegularObjects(list, array);
        return list;
    }

    /**
     * Flattens arrays of objects into the given list.
     *
     * @param array The list of JSON objects being accumulated into.
     * @param source The original JSON array data source.
     */
    private static void flattenRegularObjects(final List<JsonObject> array, final JsonArray source) {
        for (final JsonValue value: source) {
            if (value.isArray()) {
                flattenRegularObjects(array, value.asArray());
            } else if (value.isObject()) {
                array.add(value.asObject());
            }
        }
    }

    /**
     * Gets an array for the given key, or else adds a new array into the object and returns it.
     *
     * @param json  The JSON object being inspected.
     * @param field The name of the array being queried.
     * @return The existing or new array.
     */
    public static JsonArray getOrCreateArray(final JsonObject json, final String field) {
        final var value = json.get(field);
        if (value instanceof JsonArray array) {
            return array;
        } else if (value != null && !value.isNull()) {
            throw new UnsupportedOperationException("Not an array at " + field + ": " + value);
        }
        final var array = Json.array();
        json.set(field, array);
        return array;
    }

    /**
     * Gets and object for the given key, or else adds a new object into the container and returns it.
     *
     * @param json  The JSON object being inspected.
     * @param field The name of the object being queried.
     * @return The existing or new object.
     */
    public static JsonObject getOrCreateObject(final JsonObject json, final String field) {
        if (json.get(field) instanceof JsonObject object) {
            return object;
        }
        final JsonObject object = Json.object();
        json.set(field, object);
        return object;
    }
}
