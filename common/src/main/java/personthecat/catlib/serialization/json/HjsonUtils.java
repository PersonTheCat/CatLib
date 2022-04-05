package personthecat.catlib.serialization.json;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableObject;
import org.hjson.*;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.exception.JsonFormatException;
import personthecat.catlib.exception.UnreachableException;
import personthecat.catlib.serialization.codec.HjsonOps;
import personthecat.fresult.Result;
import personthecat.fresult.Void;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Optional.empty;
import static personthecat.catlib.exception.Exceptions.jsonFormatEx;
import static personthecat.catlib.exception.Exceptions.unreachable;
import static personthecat.catlib.util.Shorthand.f;
import static personthecat.catlib.util.Shorthand.full;
import static personthecat.catlib.util.Shorthand.assertEnumConstant;
import static personthecat.catlib.util.Shorthand.nullable;

/**
 * A collection of convenience methods for interacting with Hjson objects. Unlike
 * the original methods inside of {@link JsonObject}, most of the utilities in this
 * class return values wrapped in {@link Optional}, instead of <code>null</code>.
 * <p>
 *   In a future version of this library (via PersonTheCat/hjson-java), JSON objects
 *   will support returning {@link Optional} out of the box, as well as the options
 *   to flatten arrays, support additional data types, and more. As a result, most
 *   of these utilities will eventually be deprecated.
 * </p>
 */
@Log4j2
@UtilityClass
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class HjsonUtils {

    /**
     * The settings to be used when outputting JsonObjects to the disk.
     * <p>
     *   In a future version of this library, these settings will be configurable
     *   in a common config file which applies to all mods.
     * </p>
     */
    public static final HjsonOptions FORMATTER = new HjsonOptions()
        .setAllowCondense(true)
        .setAllowMultiVal(true)
        .setCommentSpace(0)
        .setSpace(2)
        .setBracesSameLine(true)
        .setOutputComments(true);

    /**
     * A second formatter and otherwise identical variant of {@link #FORMATTER} which
     * does not output <code>\r</code> characters.
     *
     * <p>
     *   This formatter is <b>ideal for use with commands</b> for a cleaner chat output.
     * </p>
     */
    public static final HjsonOptions NO_CR = new HjsonOptions()
        .setAllowCondense(true)
        .setAllowMultiVal(true)
        .setCommentSpace(0)
        .setSpace(2)
        .setBracesSameLine(true)
        .setOutputComments(true)
        .setNewLine("\n");

    /**
     * Reads a {@link JsonObject} from the given file.
     *
     * @param file The file containing the serialized JSON object.
     * @return The deserialized object, or else {@link Optional#empty}.
     */
    public static Optional<JsonObject> readJson(final File file) {
        return Result
            .define(FileNotFoundException.class, Result::WARN)
            .and(ParseException.class, e -> { throw jsonFormatEx(file.getPath(), e); })
            .suppress(() -> JsonObject.readHjson(new FileReader(file), FORMATTER).asObject())
            .get();
    }

    /**
     * Reads a {@link JsonObject} from the given input stream.
     *
     * @param is The stream containing the serialized JSON object.
     * @return The deserialized object, or else {@link Optional#empty}.
     */
    public static Optional<JsonObject> readJson(final InputStream is) {
        return Result
            .define(IOException.class, Result::WARN)
            .and(ParseException.class, r -> { throw jsonFormatEx("Reading data"); })
            .suppress(() -> JsonObject.readHjson(new InputStreamReader(is), FORMATTER).asObject())
            .get();
    }

    /**
     * Variant of {@link #readJson(File)} which ignores syntax errors
     * and simply returns {@link Optional#empty} if any error occurs.
     *
     * @param file The file containing the serialized JSON object.
     * @return The deserialized object, or else {@link Optional#empty}.
     */
    public static Optional<JsonObject> readSuppressing(final File file) {
        return Result.suppress(() -> JsonObject.readHjson(new FileReader(file), FORMATTER).asObject())
            .get(Result::WARN);
    }

    /**
     * Variant of {@link #readSuppressing(File)} which reads directly
     * from an {@link InputStream}.
     *
     * @param is The data containing the serialized JSON object.
     * @return The deserialized object, or else {@link Optional#empty}.
     */
    public static Optional<JsonObject> readSuppressing(final InputStream is) {
        return Result.suppress(() -> JsonObject.readHjson(new InputStreamReader(is), FORMATTER).asObject())
            .get(Result::WARN);
    }

    /**
     * Reads <b>any</b> JSON data from the given string contents.
     *
     * @param contents The raw JSON data being parsed.
     * @return The parsed JSON data, or else {@link Result#err} containing the exception.
     */
    public static Result<JsonValue, ParseException> readValue(final String contents) {
        return Result.<JsonValue, ParseException>of(() -> JsonObject.readHjson(contents)).ifErr(Result::IGNORE);
    }

    /**
     * Reads an object from the given data when provided a codec.
     *
     * @param codec Instructions for deserializing the data.
     * @param value The actual data being deserialized.
     * @param <T> The type of object being returned.
     * @return The deserialized object, or else {@link Optional#empty}.
     */
    public static <T> Optional<T> readOptional(final Codec<T> codec, final JsonValue value) {
        return codec.parse(HjsonOps.INSTANCE, value).result();
    }

    /**
     * Reads an object from the given data, or else throws an exception.
     *
     * @param codec Instructions for deserializing the data.
     * @param value The actual data being deserialized.
     * @param <T> The type of object being returned.
     * @return The deserialized object.
     */
    public static <T> T readThrowing(final Codec<T> codec, final JsonValue value) {
        return codec.parse(HjsonOps.INSTANCE, value).get().map(Function.identity(), partial -> {
            throw new JsonFormatException(partial.message());
        });
    }

    /**
     * Writes a regular {@link JsonObject} to the disk. The format of this output file
     * is automatically determined by its extension.
     * <p>
     *   Any file extended with <code>.json</code> will be written in regular JSON
     *   format. All other extensions will implicitly be treated as Hjson.
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
     * @return A result which potentially contains an error.
     */
    public static Result<Void, IOException> writeJson(final JsonObject json, final File file) {
        return Result.with(() -> new FileWriter(file), writer -> {
//            if (JsonType.isJson(file)) { // Write as json. todo: JsonSerializationContext.auto...
//                json.writeTo(writer, Stringify.FORMATTED);
//            } else { // Write as hjson.
//                json.writeTo(writer, FORMATTER);
//            }
        }).ifErr(e -> log.error("Writing file", e));
    }

    /**
     * Writes the input value as JSON, returning {@link Optional#empty} if any errors
     * occur in the process.
     *
     * @param codec The codec responsible for the serialization.
     * @param a The data being serialized.
     * @param <A> The type of data being serialized.
     * @return The serialized data, or else {@link Optional#empty}.
     */
    public static <A> Optional<JsonValue> writeSuppressing(final Codec<A> codec, final A a) {
        return codec.encodeStart(HjsonOps.INSTANCE, a).result();
    }

    /**
     * Writes the input value as JSON, or else throwing an exception if any errors
     * occur in the process.
     *
     * @param codec The codec responsible for the serialization.
     * @param a The data being serialized.
     * @param <A> The type of data being serialized.
     * @return The serialized data.
     */
    public static <A> JsonValue writeThrowing(final Codec<A> codec, final A a) {
        return codec.encodeStart(HjsonOps.INSTANCE, a).result()
            .orElseThrow(() -> new JsonFormatException("Writing object: " + a));
    }

    /**
     * Reads a file from the disk <em>and</em> updates it.
     * <p>
     *   For example,
     * </p>
     * <pre>{@code
     *   HjsonTools.updateJson(file, json -> {
     *      json.set("hello", "world");
     *   });
     * }</pre>
     * <p>
     *   The output of this expression will be applied to the original file.
     * </p>
     * @param file the file containing JSON data.
     * @param f Instructions for updating the JSON data.
     * @return A result which potentially contains an error.
     */
    @CheckReturnValue
    public static Result<Void, IOException> updateJson(final File file, final Consumer<JsonObject> f) {
        // If #readJson returned empty, it's because the file didn't exist.
        final JsonObject json = readJson(file).orElseGet(JsonObject::new);
        f.accept(json);
        return writeJson(json, file);
    }

    /**
     * Updates a single value in a JSON object based on a full, dotted path.
     * <p>
     *   For example,
     * </p>
     * <pre>
     *   /update my_json path.to.field true
     * </pre>
     * @param json The JSON object containing this path.
     * @param path The output of a {@link PathArgument}.
     * @param value The updated value to set at this path.
     */
    public static void setValueFromPath(final JsonObject json, final JsonPath path, @Nullable final JsonValue value) {
        if (path.isEmpty()) {
            return;
        }
        final Either<String, Integer> lastVal = path.get(path.size() - 1);
        setEither(getLastContainer(json, path), lastVal, value);
    }

    /**
     * Gets a single value in a JSON object based on a full, dotted path.
     *
     * @param json The JSON object containing this path.
     * @param path The output of a {@link PathArgument}.
     * @return The value at this location, or else {@link Optional#empty}.
     */
    public static Optional<JsonValue> getValueFromPath(final JsonObject json, final JsonPath path) {
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
     * @param json The JSON object containing this path.
     * @param path The output of a {@link PathArgument}.
     * @return The value at this location, the original <code>json</code>, or else a new container.
     */
    public static JsonValue getLastContainer(final JsonObject json, final JsonPath path) {
        if (path.isEmpty()) {
            return json;
        }
        JsonValue current = json;
        for (int i = 0; i < path.size() - 1; i++) {
            final Either<String, Integer> val = path.get(i);
            final Either<String, Integer> peek = path.get(i + 1);

            if (val.right().isPresent()) { // Index
                current = getOrTryNew(current.asArray(), val.right().get(), peek);
            } else if (peek.left().isPresent()) { // Key -> key -> object
                current = getObjectOrNew(current.asObject(), val.left()
                    .orElseThrow(UnreachableException::new));
            } else { // Key -> index -> array
                current = getArrayOrNew(current.asObject(), val.left()
                    .orElseThrow(UnreachableException::new));
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
     * @param json The JSON object containing the data being inspected.
     * @param path The path to the expected data, which may or may not exist.
     * @return The index to the last matching element, or else -1.
     */
    public static int getLastAvailable(final JsonObject json, final JsonPath path) {
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
     * @param json The object being inspected.
     * @param path The canonicalized path to the expected value
     * @return The actual path to the value, or else the canonical path.
     */
    public static JsonPath getClosestMatch(final JsonObject json, final JsonPath path) {
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
        final JsonObject generated = (JsonObject) new JsonObject().copyComments(json);
        final StringBuilder skipped = new StringBuilder();

        for (final JsonObject.Member member : json) {
            final JsonValue value = member.getValue();
            final String name = member.getName();

            if (value.isAccessed() != used) {
                if (skipped.length() > 0) {
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
            } else if (skipped.length() == 0) {
                skipped.append(name);
            } else {
                skipped.append(", ").append(name);
            }
        }
        if (skipped.length() > 0) {
            generated.prependInteriorComment("Skipped " + skipped);
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
        final JsonArray generated = (JsonArray) new JsonArray().copyComments(json);
        int lastIndex = 0;
        int index = 0;

        for (final JsonValue value : json) {
            if (value.isAccessed() != used) {
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
            generated.prependInteriorComment("Skipped " + (index - 1));
        } else if (index > lastIndex) {
            generated.prependInteriorComment("Skipped " + lastIndex + " ~ " + (index - 1));
        }
        return generated;
    }

    /**
     * Updates every value in the given JSON with values from <code>toSet</code>. Any
     * value not present in the original JSON will be copied from <code>toSet</code>.
     * <p>
     *   For example, when given the following JSON inputs:
     * </p><pre>
     *   {
     *     "a": 1,
     *     "b": {}
     *   }
     * </pre><p>
     *   and
     * </p><pre>
     *   {
     *     "a": 2,
     *     "b": {
     *        "c": 3
     *     },
     *     "d": 4
     *   }
     * </pre><p>
     *   The <b>original JSON</b> will be updated as follows:
     * </p><pre>
     *   {
     *     "a": 1,
     *     "b": {
     *       "c": 3
     *     }
     *     "d": 4
     *   }
     * </pre>
     *
     * @param json The target JSON being modified.
     * @param toSet The data being copied into the target.
     */
    public static void setRecursivelyIfAbsent(final JsonObject json, final JsonObject toSet) {
        for (final JsonObject.Member member : toSet) {
            final JsonValue get = json.get(member.getName());
            if (get != null) {
                if (get.isObject() && member.getValue().isObject()) {
                    setRecursivelyIfAbsent(get.asObject(), member.getValue().asObject());
                } else if (get.isArray() && member.getValue().isArray()) {
                    setRecursivelyIfAbsent(get.asArray(), member.getValue().asArray());
                }
            } else if (!member.getValue().isNull()) {
                json.set(member.getName(), member.getValue());
            }
        }
    }

    /**
     * Variant of {@link #setRecursivelyIfAbsent(JsonObject, JsonObject)} accepting arrays.
     *
     * @param json The target JSON being modified.
     * @param toSet The data being copied into the target.
     */
    public static void setRecursivelyIfAbsent(final JsonArray json, final JsonArray toSet) {
        final int len = Math.min(json.size(), toSet.size());
        int i = 0;
        while (i < len) {
            final JsonValue get = json.get(i);
            final JsonValue set = toSet.get(i);
            if (get.isObject() && set.isObject()) {
                setRecursivelyIfAbsent(get.asObject(), set.asObject());
            } else if (get.isArray() && set.isArray()) {
                setRecursivelyIfAbsent(get.asArray(), set.asArray());
            }
            i++;
        }
        while (i < toSet.size()) {
            json.add(toSet.get(i));
            i++;
        }
    }

    /**
     * Variant of {@link #setRecursivelyIfAbsent(JsonObject, JsonObject)} accepting unknown types.
     *
     * @param json The target JSON being modified.
     * @param toSet The data being copied into the target.
     */
    public static void setRecursivelyIfAbsent(final JsonValue json, final JsonValue toSet) {
        if (json.isObject() && toSet.isObject()) {
            setRecursivelyIfAbsent(json.asObject(), toSet.asObject());
        } else if (json.isArray() && toSet.isArray()) {
            setRecursivelyIfAbsent(json.asArray(), toSet.asArray());
        }
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
        final JsonValue container = Result.of(() -> getLastContainer(json, path))
            .get(Result::WARN)
            .orElse(json);
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
                final String name = member.getName();
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
    private static JsonValue getOrTryNew(final JsonArray array, final int index, final Either<String, Integer> type) {
        if (index == array.size()) { // The value must be added.
            type.ifLeft(s -> array.add(new JsonObject()))
                .ifRight(i -> array.add(new JsonArray()));
        } // if index >= newSize -> index out of bounds
        return array.get(index);
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
        if (either.left().isPresent()) {
            return nullable(container.asObject().get(either.left().get()));
        } else if (either.right().isPresent()) {
            final JsonArray array = container.asArray();
            final int index = either.right().get();
            return index < array.size() ? full(array.get(index)) : empty();
        }
        throw unreachable();
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
                object.set(key, value.copyComments(object.get(key)));
            }
        } else if (either.right().isPresent()) { // Just to stop the linting.
            if (value == null) {
                container.asArray().remove(either.right().get());
            } else if (value.hasComments()) {
                container.asArray().set(either.right().get(), value);
            } else {
                final int index = either.right().get();
                final JsonArray array = container.asArray();
                array.set(index, value.copyComments(array.get(index)));
            }
        }
    }

    /**
     * Creates a new {@link JsonArray} from a collection of strings.
     *
     * @param strings The contents of the array.
     * @return A regular {@link JsonArray}.
     */
    public static JsonArray stringArray(final Collection<String> strings) {
        return createArray(strings, JsonArray::add);
    }

    /**
     * Creates a new {@link JsonArray} from a collection of strings.
     *
     * @param ints The contents of the array.
     * @return A regular {@link JsonArray}.
     */
    public static JsonArray intArray(final Collection<Integer> ints) {
        return createArray(ints, JsonArray::add);
    }

    /**
     * Creates a new {@link JsonArray} from a collection of doubles.
     *
     * @param doubles The contents of the array.
     * @return A regular {@link JsonArray}.
     */
    public static JsonArray doubleArray(final Collection<Double> doubles) {
        return createArray(doubles, JsonArray::add);
    }

    /**
     * Creates a new {@link JsonArray} from a collection of floats.
     *
     * @param floats The contents of the array.
     * @return A regular {@link JsonArray}.
     */
    public static JsonArray floatArray(final Collection<Float> floats) {
        return createArray(floats, JsonArray::add);
    }

    /**
     * Creates a new {@link JsonArray} from a collection of booleans.
     *
     * @param booleans The contents of the array.
     * @return A regular {@link JsonArray}.
     */
    public static JsonArray boolArray(final Collection<Boolean> booleans) {
        return createArray(booleans, JsonArray::add);
    }

    /**
     * Creates a new {@link JsonArray} from a collection of {@link JsonValue}s.
     *
     * @param values The contents of the array.
     * @return A regular {@link JsonArray}.
     */
    public static JsonArray valueArray(final Collection<? extends JsonValue> values) {
        return createArray(values, JsonArray::add);
    }

    /**
     * Creates a new {@link JsonArray} from a collection of any type.
     *
     * @param any The contents of the array.
     * @return A regular {@link JsonArray}.
     */
    public static JsonArray anyArray(final Collection<?> any) {
        return createArray(any, (a, o) -> a.add(JsonValue.valueOf(o)));
    }

    /**
     * Generates a {@link JsonArray} containing values of the given type.
     *
     * @param values A collection of any type which the array will be constructed from.
     * @param adder  Instructions for how to add the elements into the array.
     * @param <T>    The type of values in the collection.
     * @return A regular {@link JsonArray}.
     */
    public static <T> JsonArray createArray(final Collection<T> values, final BiConsumer<JsonArray, T> adder) {
        final JsonArray array = new JsonArray();
        values.forEach(t -> adder.accept(array, t));
        return array;
    }

    /**
     * Adds a value to an array by name. The value will be coerced into an array, if needed.
     * <p>
     *   For example, when adding a string to the following JSON field:
     * </p>
     * <pre>
     *   field: hello
     * </pre>
     * <p>
     *   the field will be updated as follows:
     * </p>
     * <pre>
     *   field: [
     *     hello
     *     world
     *   ]
     * </pre>
     * @param json The JSON object containing these data.
     * @param field The key for updating an array.
     * @param value The value being added to the array.
     * @return The original <code>json</code> passed in.
     */
    public static JsonObject addToArray(final JsonObject json, final String field, final JsonValue value) {
        JsonValue array = json.get(field);
        if (array == null) {
            array = new JsonArray();
            json.add(field, array);
        } else if (!array.isArray()) {
            array = new JsonArray().add(array);
            json.set(field, array);
        }
        array.asArray().add(value);
        return json;
    }

    /**
     * Returns a list of {@link JsonObject}s from the given source.
     * <p>
     *   Note that the values in this array will be coerced into {@link JsonObject}s.
     * </p>
     * <p>
     *   These objects can be stored in any number of dimensions, but will be coerced
     *   into a single dimensional array. For example, each of the following values will
     *   yield single dimensional object arrays:
     * </p>
     * <ul>
     *   <li><code>array: [{},{},{}]</code></li>
     *   <li><code>array: [[{}],[[{}]]]</code></li>
     *   <li><code>array: {}</code></li>
     * </ul>
     * @param json The JSON parent containing the array.
     * @param field The field where this array is stored.
     * @return The JSON array in the form of a regular list.
     */
    public static List<JsonObject> getObjectArray(final JsonObject json, final String field) {
        final List<JsonObject> array = new ArrayList<>();
        getValue(json, field).map(HjsonUtils::asOrToArray)
            .ifPresent(a -> flatten(array, a));
        return array;
    }

    /**
     * Recursively flattens object arrays into a single dimension.
     *
     * @param array The list of JSON objects being accumulated into.
     * @param source The original JSON array data source.
     */
    private static void flatten(final List<JsonObject> array, final JsonArray source) {
        for (final JsonValue value: source) {
            if (value.isArray()) {
                flatten(array, value.asArray());
            } else if (value.isObject()) {
                array.add(value.asObject());
            } else {
                throw jsonFormatEx("Expected an array or object: {}", value);
            }
        }
    }

    /**
     * Variant of {@link #getObjectArray} which does not coerce values into objects.
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
        final JsonArray array = HjsonUtils.getValue(json, field)
            .map(HjsonUtils::asOrToArray)
            .orElseGet(JsonArray::new);
        flattenRegularObjects(list, array);
        return list;
    }

    /**
     * Variant of {@link #flatten} which does not coerce values into objects.
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

    public static Optional<Boolean> getBool(final JsonObject json, final String field) {
        return getValue(json, field).map(JsonValue::asBoolean);
    }

    public static Optional<Integer> getInt(final JsonObject json, final String field) {
        return getValue(json, field).map(JsonValue::asInt);
    }

    public static Optional<Float> getFloat(final JsonObject json, final String field) {
        return getValue(json, field).map(JsonValue::asFloat);
    }

    public static Optional<String> getString(final JsonObject json, final String field) {
        return getValue(json, field).map(JsonValue::asString);
    }

    public static Optional<JsonArray> getArray(final JsonObject json, final String field) {
        return getValue(json, field).map(HjsonUtils::asOrToArray);
    }

    public static JsonArray getArrayOrNew(final JsonObject json, final String field) {
        if (!json.has(field)) {
            json.set(field, new JsonArray());
        }
        return getArray(json, field).orElseThrow(UnreachableException::new);
    }

    public static JsonArray asOrToArray(final JsonValue value) {
        return value.isArray() ? value.asArray() : new JsonArray().add(value);
    }

    public static Optional<JsonObject> getObject(final JsonObject json, final String field) {
        return getValue(json, field).map(JsonValue::asObject);
    }

    public static JsonObject getObjectOrNew(final JsonObject json, final String field) {
        JsonValue get = json.get(field);
        if (get == null) {
            json.set(field, get = new JsonObject());
        }
        return get.asObject();
    }

    public static Optional<JsonValue> getValue(final JsonObject json, final String field) {
        return Optional.ofNullable(json.get(field));
    }

    private static <T extends Enum<T>> List<T> toEnumArray(final JsonArray array, final Class<T> clazz) {
        final List<T> list = new ArrayList<>();
        for (final JsonValue value : array) {
            list.add(assertEnumConstant(value.asString(), clazz));
        }
        return list;
    }
}
