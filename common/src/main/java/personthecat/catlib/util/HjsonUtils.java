package personthecat.catlib.util;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.hjson.*;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.command.arguments.PathArgument;
import personthecat.catlib.data.*;
import personthecat.catlib.data.JsonType;
import personthecat.catlib.exception.UnreachableException;
import personthecat.fresult.Result;
import personthecat.fresult.Void;

import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static personthecat.catlib.exception.Exceptions.jsonFormatEx;
import static personthecat.catlib.exception.Exceptions.noBiomeNamed;
import static personthecat.catlib.exception.Exceptions.noBiomeTypeNamed;
import static personthecat.catlib.exception.Exceptions.noBlockNamed;
import static personthecat.catlib.exception.Exceptions.runEx;
import static personthecat.catlib.exception.Exceptions.unreachable;
import static personthecat.catlib.util.McUtils.getBiome;
import static personthecat.catlib.util.McUtils.getBiomes;
import static personthecat.catlib.util.McUtils.getBiomeType;
import static personthecat.catlib.util.McUtils.parseBlockState;
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
            .and(ParseException.class, e -> { throw runEx(file.getPath(), e); })
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
            .and(ParseException.class, Result::THROW)
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
        return Result.of(() -> JsonObject.readHjson(new FileReader(file), FORMATTER).asObject())
            .get(Result::WARN);
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
     *   logged and simply returned for the calsler to optionally throw.
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
            if (JsonType.isJson(file)) { // Write as json.
                json.writeTo(writer, Stringify.FORMATTED);
            } else { // Write as hjson.
                json.writeTo(writer, FORMATTER);
            }
        }).ifErr(e -> log.error("Writing file", e));
    }

    /**
     * Reads a file from the disk <em>and</em> updates it.
     * <p>
     *   For example,
     * </p>
     * <pre>
     *   HjsonTools.updateJson(file, json -> {
     *      json.set("hello", "world");
     *   });
     * </pre>
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
    public static void setValueFromPath(final JsonObject json, final PathArgument.Result path, @Nullable final JsonValue value) {
        if (path.path.isEmpty()) {
            return;
        }
        final Either<String, Integer> lastVal = path.path.get(path.path.size() - 1);
        setEither(getLastContainer(json, path), lastVal, value);
    }

    /**
     * Gets a single value in a JSON object based on a full, dotted path.
     *
     * @param json The JSON object containing this path.
     * @param path The output of a {@link PathArgument}.
     * @return The value at this location, or else {@link Optional#empty}.
     */
    public static Optional<JsonValue> getValueFromPath(final JsonObject json, final PathArgument.Result path) {
        if (path.path.isEmpty()) {
            return empty();
        }
        final Either<String, Integer> lastVal = path.path.get(path.path.size() - 1);
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
    public static JsonValue getLastContainer(final JsonObject json, final PathArgument.Result path) {
        if (path.path.isEmpty()) {
            return json;
        }
        JsonValue current = json;
        for (int i = 0; i < path.path.size() - 1; i++) {
            final Either<String, Integer> val = path.path.get(i);
            final Either<String, Integer> peek = path.path.get(i + 1);

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
    public static List<String> getPaths(final JsonObject json, final PathArgument.Result path) {
        final JsonValue container = Result.of(() -> getLastContainer(json, path))
            .get(Result::WARN)
            .orElse(json);
        int end = path.path.size() - 1;
        if (end < 0) {
            return getNeighbors("", container);
        }
        final Optional<JsonValue> v = getEither(container, path.path.get(end))
            .filter(value -> value.isObject() || value.isArray());
        if (v.isPresent()) {
            end++; // The full path is a valid container -> use it.
        }
        final String dir = PathArgument.serialize(path.path.subList(0, end));
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
            } else {
                container.asObject().set(either.left().get(), value);
            }
        } else if (either.right().isPresent()) { // Just to stop the linting.
            if (value == null) {
                container.asArray().remove(either.right().get());
            } else {
                container.asArray().set(either.right().get(), value);
            }
        }
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
     * @value The value being added to the array.
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
     * </p
     * <p>
     *   These objects can be stored in any number of dimensions, but will be coerced
     *   into a single dimensional array. For example, all of the following values will
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

    public static Optional<Range> getRange(final JsonObject json, final String field) {
        return getValue(json, field)
            .map(HjsonUtils::asOrToArray)
            .map(HjsonUtils::toIntArray)
            .map(Shorthand::sort)
            .map(HjsonUtils::toRange);
    }

    private static Range toRange(final int[] range) {
        if (range.length == 0) {
            return EmptyRange.get();
        }
        return range.length == 1 ? new Range(range[0]) : new Range(range[0], range[range.length - 1]);
    }

    public static Optional<FloatRange> getFloatRange(final JsonObject json, final String field) {
        return getValue(json, field)
            .map(HjsonUtils::asOrToArray)
            .map(HjsonUtils::toFloatArray)
            .map(Shorthand::sort)
            .map(HjsonUtils::toFloatRange);
    }

    private static FloatRange toFloatRange(float[] range) {
        if (range.length == 0) {
            return new FloatRange(0F);
        }
        return range.length == 1 ? new FloatRange(range[0]) : new FloatRange(range[0], range[range.length -1]);
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
        if (!json.has(field)) {
            json.set(field, new JsonObject());
        }
        return getObject(json, field).orElseThrow(() -> runEx("Unreachable."));
    }

    public static Optional<JsonValue> getValue(final JsonObject json, final String field) {
        return Optional.ofNullable(json.get(field));
    }

    public static Optional<IntList> getIntList(final JsonObject json, final String field) {
        return getArray(json, field).map(HjsonUtils::toIntList);
    }

    private static IntList toIntList(final JsonArray array) {
        final IntList ints = new IntArrayList();
        for (final JsonValue value : array) {
            ints.add(value.asInt());
        }
        return ints;
    }

    public static int[] toIntArray(final JsonArray array) {
        final int[] ints = new  int[array.size()];
        for (int i = 0; i < array.size(); i++) {
            ints[i] = array.get(i).asInt();
        }
        return ints;
    }

    public static Optional<FloatList> getFloatList(final JsonObject json, final String field) {
        return getArray(json, field).map(HjsonUtils::toFloatList);
    }

    private static FloatList toFloatList(final JsonArray array) {
        final FloatList floats = new FloatArrayList();
        for (final JsonValue value : array) {
            floats.add(value.asFloat());
        }
        return floats;
    }

    public static float[] toFloatArray(final JsonArray array) {
        final float[] floats = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            floats[i] = array.get(i).asFloat();
        }
        return floats;
    }

    public static Optional<List<String>> getStringArray(final JsonObject json, final String field) {
        return getValue(json, field).map(v -> toStringArray(asOrToArray(v)));
    }

    public static List<String> toStringArray(JsonArray array) {
        final List<String> strings = new ArrayList<>();
        for (final JsonValue value : array) {
            strings.add(value.asString());
        }
        return strings;
    }

    public static Optional<ResourceLocation> getId(final JsonObject json, final String field) {
        return getString(json, field).map(ResourceLocation::new);
    }

    public static Optional<List<ResourceLocation>> getIds(final JsonObject json, final String field) {
        return getArray(json, field).map(HjsonUtils::toIds);
    }

    public static List<ResourceLocation> toIds(final JsonArray array) {
        final List<ResourceLocation> ids = new ArrayList<>();
        for (final JsonValue value : array) {
            ids.add(new ResourceLocation(value.asString()));
        }
        return ids;
    }

    public static Optional<BlockState> getState(final JsonObject json, final String field) {
        return getString(json, field).map(id -> parseBlockState(id).orElseThrow(() -> noBlockNamed(id)));
    }

    public static Optional<List<BlockState>> getStateList(final JsonObject json, final String field) {
        return getStringArray(json, field).map(HjsonUtils::toStateList);
    }

    private static List<BlockState> toStateList(final List<String> ids) {
        return ids.stream().map(id -> parseBlockState(id).orElseThrow(() -> noBlockNamed(id)))
            .collect(Collectors.toList());
    }

    public static Optional<BlockPos> getPosition(JsonObject json, String field) {
        return getArray(json, field).map(HjsonUtils::toPosition);
    }

    public static Optional<List<BlockPos>> getPositionList(JsonObject json, String field) {
        return getArray(json, field).map(HjsonUtils::toPositionList);
    }

    public static BlockPos toPosition(final JsonArray coordinates) {
        // Expect exactly 3 elements.
        if (coordinates.size() != 3) {
            throw jsonFormatEx("Relative coordinates must be specified in an array of 3 elements, e.g. [0, 0, 0].");
        }
        // Convert the array into a BlockPos object.
        return new BlockPos(coordinates.get(0).asInt(), coordinates.get(1).asInt(), coordinates.get(2).asInt());
    }

    private static List<BlockPos> toPositionList(final JsonArray positions) {
        final List<BlockPos> list = new ArrayList<>();
        for (JsonValue position : positions) {
            if (position.isNumber()) {
                return Collections.singletonList(toPosition(positions));
            } else if (!position.isArray()) {
                throw runEx("Expected a list of positions, e.g. [[0, 0, 0], [1, 1, 1]].");
            }
            list.add(toPosition(position.asArray()));
        }
        return list;
    }

    public static Optional<BiomePredicate> getBiomePredicate(final JsonObject json, final String field) {
        return getObject(json, field).map(HjsonUtils::toBiomePredicate);
    }

    public static BiomePredicate toBiomePredicate(final JsonValue json) {
        final BiomePredicate.BiomePredicateBuilder builder = BiomePredicate.builder();

        if (json.isObject()) {
            final JsonObject o = json.asObject();
            getIds(o, "names").ifPresent(builder::names);
            getBiomeTypes(o, "types").ifPresent(builder::types);
            getStringArray(o, "mods").ifPresent(builder::mods);
            getBool(o, "blacklist").ifPresent(builder::blacklist);
        } else {
            builder.names(toIds(asOrToArray(json)));
        }

        return builder.build();
    }

    public static Optional<List<Biome>> getBiomeList(final JsonObject json, final String field) {
        return getValue(json, field).map(HjsonUtils::toBiomes);
    }

    public static List<Biome> toBiomes(final JsonValue json) {
        final List<Biome> biomes = new ArrayList<>();

        if (json.isObject()) {
            final JsonObject o = json.asObject();
            // Get biomes by registry name.
            getIds(o, "names").ifPresent(a -> {
                for (final ResourceLocation id : a) {
                    biomes.add(getBiome(id).orElseThrow(() -> noBiomeNamed(id.toString())));
                }
            });
            // Get biomes by type.
            getBiomeTypes(o, "types").ifPresent(a -> {
                for (final Biome.BiomeCategory t : a) {
                    biomes.addAll(getBiomes(t));
                }
            });
        } else {
            for (final ResourceLocation id : toIds(asOrToArray(json))) {
                biomes.add(getBiome(id).orElseThrow(() -> noBiomeNamed(id.toString())));
            }
        }
        return biomes;
    }

    public static Optional<List<Biome.BiomeCategory>> getBiomeTypes(final JsonObject json, final String field) {
        return getArray(json, field).map(HjsonUtils::toBiomeTypes);
    }

    public static List<Biome.BiomeCategory> toBiomeTypes(final JsonArray array) {
        List<Biome.BiomeCategory> types = new ArrayList<>();
        for (final JsonValue value : array) {
            final String s = value.asString();
            types.add(getBiomeType(s).orElseThrow(() -> noBiomeTypeNamed(s)));
        }
        return types;
    }

    public static Optional<DimensionPredicate> getDimensionPredicate(final JsonObject json, final String field) {
        return getValue(json, field).map(HjsonUtils::toDimensions);
    }

    public static DimensionPredicate toDimensions(final JsonValue json) {
        final DimensionPredicate.DimensionPredicateBuilder builder = DimensionPredicate.builder();

        if (json.isObject()) {
            final JsonObject o = json.asObject();
            getIds(o, "names").ifPresent(builder::names);
            getStringArray(o, "mods").ifPresent(builder::mods);
        } else {
            builder.names(toIds(asOrToArray(json)));
        }
        return builder.build();
    }

    public static StructurePlaceSettings getPlacementSettings(final JsonObject json) {
        final StructurePlaceSettings settings = new StructurePlaceSettings();
        getFloat(json, "integrity").ifPresent(i -> settings.addProcessor(new BlockRotProcessor(i)));
        getEnumValue(json, "mirror", Mirror.class).ifPresent(settings::setMirror);
        getBool(json, "ignoreEntities").ifPresent(settings::setIgnoreEntities);
        getBool(json, "hasGravity").ifPresent(b ->
            settings.addProcessor(new GravityProcessor(Heightmap.Types.MOTION_BLOCKING, 1)));

        return settings;
    }

    public static <T extends Enum<T>> Optional<T> getEnumValue(final JsonObject json, final String field, final Class<T> clazz) {
        return getString(json, field).map(s -> assertEnumConstant(s, clazz));
    }

    public static <T extends Enum<T>> Optional<List<T>> getEnumList(final JsonObject json, final String field, final Class<T> clazz) {
        return getValue(json, field).map(v -> toEnumArray(asOrToArray(v), clazz));
    }

    private static <T extends Enum<T>> List<T> toEnumArray(final JsonArray array, final Class<T> clazz) {
        final List<T> list = new ArrayList<>();
        for (final JsonValue value : array) {
            list.add(assertEnumConstant(value.asString(), clazz));
        }
        return list;
    }
}
