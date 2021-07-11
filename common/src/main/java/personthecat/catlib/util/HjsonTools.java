package personthecat.catlib.util;

import com.mojang.datafixers.util.Either;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.hjson.*;
import personthecat.catlib.command.PathArgument;
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
import static personthecat.catlib.util.McTools.getBiome;
import static personthecat.catlib.util.McTools.getBiomes;
import static personthecat.catlib.util.McTools.getBiomeType;
import static personthecat.catlib.util.McTools.getBlockState;
import static personthecat.catlib.util.PathTools.extension;
import static personthecat.catlib.util.Shorthand.f;
import static personthecat.catlib.util.Shorthand.full;
import static personthecat.catlib.util.Shorthand.getEnumConstant;
import static personthecat.catlib.util.Shorthand.nullable;

@Log4j2
@UtilityClass
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
public class HjsonTools {

    /** The settings to be used when outputting JsonObjects to the disk. */
    public static final HjsonOptions FORMATTER = new HjsonOptions()
        .setAllowCondense(true)
        .setAllowMultiVal(true)
        .setCommentSpace(0)
        .setSpace(2)
        .setBracesSameLine(true)
        .setOutputComments(true);

    public static Optional<JsonObject> readJson(final File file) {
        return Result
            .define(FileNotFoundException.class, Result::WARN)
            .and(ParseException.class, e -> { throw runEx(file.getPath(), e); })
            .suppress(() -> JsonObject.readHjson(new FileReader(file), FORMATTER).asObject())
            .get();
    }

    public static Optional<JsonObject> readJson(final InputStream is) {
        return Result
            .define(IOException.class, Result::WARN)
            .and(ParseException.class, Result::THROW)
            .suppress(() -> JsonObject.readHjson(new InputStreamReader(is), FORMATTER).asObject())
            .get();
    }


    /** Writes the JsonObject to the disk. */
    public static Result<Void, IOException> writeJson(final JsonObject json, final File file) {
        return Result.with(() -> new FileWriter(file), writer -> {
            if (extension(file).equals("json")) { // Write as json.
                json.writeTo(writer, Stringify.FORMATTED);
            } else { // Write as hjson.
                json.writeTo(writer, FORMATTER);
            }
        }).ifErr(e -> log.error("Writing file", e));
    }

    /** Reads a file from the disk <em>and</em> updates it. */
    @CheckReturnValue
    public static Result<Void, IOException> updateJson(final File file, final Consumer<JsonObject> f) {
        // If #readJson returned empty, it's because the file didn't exist.
        final JsonObject json = readJson(file).orElseGet(JsonObject::new);
        f.accept(json);
        return writeJson(json, file);
    }

    /** Updates a single value in a json based on a full, dotted path.  */
    public static void setValueFromPath(final JsonObject json, final PathArgument.Result path, final JsonValue value) {
        if (path.path.isEmpty()) {
            return;
        }
        final Either<String, Integer> lastVal = path.path.get(path.path.size() - 1);
        setEither(getLastContainer(json, path), lastVal, value);
    }

    /** Attempts to retrieve the value referenced by `path`. */
    public static Optional<JsonValue> getValueFromPath(final JsonObject json, final PathArgument.Result path) {
        if (path.path.isEmpty()) {
            return empty();
        }
        final Either<String, Integer> lastVal = path.path.get(path.path.size() - 1);
        return getEither(getLastContainer(json, path), lastVal);
    }

    /** Retrieves the last JsonObject or JsonArray represented by the path. */
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

    /** Retrieves a list of paths adjacent to `path`. */
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

    /** Retrieves a list of paths in `container`. */
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

    /** Attempts to retrieve an object or an array. Creates a new one, if absent. */
    private static JsonValue getOrTryNew(final JsonArray array, final int index, final Either<String, Integer> type) {
        if (index == array.size()) { // The value must be added.
            type.ifLeft(s -> array.add(new JsonObject()))
                .ifRight(i -> array.add(new JsonArray()));
        } // if index >= newSize -> index out of bounds
        return array.get(index);
    }

    /** Attempts to retrieve either an object or an array from an object. */
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

    /** Attempts to set a value in a container which may either be an object or an array. */
    private static void setEither(final JsonValue container, final Either<String, Integer> either, final JsonValue value) {
        if (either.left().isPresent()) {
            container.asObject().set(either.left().get(), value);
        } else if (either.right().isPresent()) { // Just to stop the linting.
            container.asArray().set(either.right().get(), value);
        }
    }

    /** Adds a value to an array by name. The value will be coerced into an array, if needed.*/
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

    /** Safely retrieves a boolean from the input object. */
    public static Optional<Boolean> getBool(final JsonObject json, final String field) {
        return getValue(json, field).map(JsonValue::asBoolean);
    }

    /** Safely retrieves an integer from the input json. */
    public static Optional<Integer> getInt(final JsonObject json, final String field) {
        return getValue(json, field).map(JsonValue::asInt);
    }

    public static int getIntOr(final JsonObject json, final String field, final int orElse) {
        return getInt(json, field).orElse(orElse);
    }

    /** Retrieves a range of integers from the input object. */
    public static Optional<Range> getRange(final JsonObject json, final String field) {
        return getValue(json, field)
            .map(HjsonTools::asOrToArray)
            .map(HjsonTools::toIntArray)
            .map(Shorthand::sort)
            .map(HjsonTools::toRange);
    }

    private static Range toRange(final int[] range) {
        if (range.length == 0) {
            return EmptyRange.get();
        }
        return range.length == 1 ? new Range(range[0]) : new Range(range[0], range[range.length - 1]);
    }

    public static Optional<FloatRange> getFloatRange(final JsonObject json, final String field) {
        return getValue(json, field)
            .map(HjsonTools::asOrToArray)
            .map(HjsonTools::toFloatArray)
            .map(Shorthand::sort)
            .map(HjsonTools::toFloatRange);
    }

    private static FloatRange toFloatRange(float[] range) {
        if (range.length == 0) {
            return new FloatRange(0F);
        }
        return range.length == 1 ? new FloatRange(range[0]) : new FloatRange(range[0], range[range.length -1]);
    }

    /** Safely retrieves a boolean from the input json. */
    public static Optional<Float> getFloat(final JsonObject json, final String field) {
        return getValue(json, field).map(JsonValue::asFloat);
    }

    /** Retrieves a float from the input object. Returns `or` if nothing is found. */
    public static float getFloatOr(final JsonObject json, final String field, final float orElse) {
        return getFloat(json, field).orElse(orElse);
    }

    /** Safely retrieves a string from the input json. */
    public static Optional<String> getString(final JsonObject json, final String field) {
        return getValue(json, field).map(JsonValue::asString);
    }

    /** Safely retrieves a JsonArray from the input json. */
    public static Optional<JsonArray> getArray(final JsonObject json, final String field) {
        return getValue(json, field).map(HjsonTools::asOrToArray);
    }

    /**  Retrieves an array or creates a new one, if absent. */
    public static JsonArray getArrayOrNew(final JsonObject json, final String field) {
        if (!json.has(field)) {
            json.set(field, new JsonArray());
        }
        return getArray(json, field).orElseThrow(UnreachableException::new);
    }

    /** Casts or converts a JsonValue to a JsonArray.*/
    public static JsonArray asOrToArray(final JsonValue value) {
        return value.isArray() ? value.asArray() : new JsonArray().add(value);
    }

    /** Safely retrieves a boolean from the input json. */
    public static Optional<JsonObject> getObject(final JsonObject json, final String field) {
        return getValue(json, field).map(JsonValue::asObject);
    }

    /** Retrieves an object from the input object. Returns an empty object, if nothing is found. */
    public static JsonObject getObjectOrNew(final JsonObject json, final String field) {
        if (!json.has(field)) {
            json.set(field, new JsonObject());
        }
        return getObject(json, field).orElseThrow(() -> runEx("Unreachable."));
    }

    /** Safely retrieves a JsonValue from the input object. */
    public static Optional<JsonValue> getValue(final JsonObject json, final String field) {
        return Optional.ofNullable(json.get(field));
    }

    /**
     * Safely retrieves an array of JsonObjects from the input json.
     */
    public static List<JsonObject> getObjectArray(final JsonObject json, final String field) {
        final List<JsonObject> array = new ArrayList<>();
        getValue(json, field).map(HjsonTools::asOrToArray)
            .ifPresent(a -> flatten(array, a));
        return array;
    }

    /** Recursively flattens object arrays into a single dimension. */
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

    /** Variant of {@link #getObjectArray} which does not coerce values into objects. */
    public static List<JsonObject> getRegularObjects(final JsonObject json, final String field) {
        final List<JsonObject> list = new ArrayList<>();
        final JsonArray array = HjsonTools.getValue(json, field)
            .map(HjsonTools::asOrToArray)
            .orElseGet(JsonArray::new);
        flattenRegularObjects(list, array);
        return list;
    }

    /** Variant of {@link #flatten} which does not coerce values into objects. */
    private static void flattenRegularObjects(final List<JsonObject> array, final JsonArray source) {
        for (final JsonValue value: source) {
            if (value.isArray()) {
                flattenRegularObjects(array, value.asArray());
            } else if (value.isObject()) {
                array.add(value.asObject());
            }
        }
    }

    public static Optional<List<Integer>> getIntList(final JsonObject json, final String field) {
        return getArray(json, field).map(HjsonTools::toIntList);
    }

    private static List<Integer> toIntList(final JsonArray array) {
        final List<Integer> ints = new ArrayList<>();
        for (final JsonValue value : array) {
            if (!value.isNumber()) {
                throw runEx("Expected an numeric value: {}", value);
            }
            ints.add(value.asInt());
        }
        return ints;
    }

    /** Converts a JsonArray into an array of ints. */
    public static int[] toIntArray(final JsonArray array) {
        final int[] ints = new  int[array.size()];
        for (int i = 0; i < array.size(); i++) {
            ints[i] = array.get(i).asInt();
        }
        return ints;
    }

    public static float[] toFloatArray(final JsonArray array) {
        final float[] floats = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            floats[i] = array.get(i).asFloat();
        }
        return floats;
    }

    /** Safely retrieves a List of Strings from the input json. */
    public static Optional<List<String>> getStringArray(final JsonObject json, final String field) {
        return getValue(json, field).map(v -> toStringArray(asOrToArray(v)));
    }

    /** Converts a JsonArray into a List of Strings. */
    public static List<String> toStringArray(JsonArray array) {
        final List<String> strings = new ArrayList<>();
        for (final JsonValue value : array) {
            strings.add(value.asString());
        }
        return strings;
    }

    public static Optional<BlockState> getState(final JsonObject json, final String field) {
        return getString(json, field).map(id -> getBlockState(id).orElseThrow(() -> noBlockNamed(id)));
    }

    public static Optional<List<BlockState>> getStateList(final JsonObject json, final String field) {
        return getStringArray(json, field).map(HjsonTools::toStateList);
    }

    private static List<BlockState> toStateList(final List<String> ids) {
        return ids.stream().map(id -> getBlockState(id).orElseThrow(() -> noBlockNamed(id)))
            .collect(Collectors.toList());
    }

    /** Safely retrieves a BlockPos from the input object. */
    public static Optional<BlockPos> getPosition(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toPosition);
    }

    public static Optional<List<BlockPos>> getPositionList(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toPositionList);
    }

    /** Converts the input JsonArray into a BlockPos object. */
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

    public static Optional<List<Biome>> getBiomeList(final JsonObject json, final String field) {
        return getObject(json, field).map(HjsonTools::toBiomes);
    }

    private static List<Biome> toBiomes(final JsonObject json) {
        final List<Biome> biomes = new ArrayList<>();
        // Get biomes by registry name.
        getArray(json, "names").map(HjsonTools::toStringArray).ifPresent(a -> {
            for (final String s : a) {
                biomes.add(getBiome(s).orElseThrow(() -> noBiomeNamed(s)));
            }
        });
        // Get biomes by type.
        getArray(json, "types").map(HjsonTools::toBiomeTypes).ifPresent(a -> {
            for (final Biome.BiomeCategory t : a) {
                biomes.addAll(getBiomes(t));
            }
        });
        return biomes;
    }

    /** Converts a JsonArray in to a list of BiomeTypes. */
    public static List<Biome.BiomeCategory> toBiomeTypes(final JsonArray array) {
        List<Biome.BiomeCategory> types = new ArrayList<>();
        for (final JsonValue value : array) {
            final String s = value.asString();
            types.add(getBiomeType(s).orElseThrow(() -> noBiomeTypeNamed(s)));
        }
        return types;
    }

    /**
     * Constructs the standard PlacementSettings object used by the mod,
     * apply additional values, when possible.
     */
    public static StructureBlockEntity getPlacementSettings(final JsonObject json) {
        final StructureBlockEntity settings = new StructureBlockEntity();
        getFloat(json, "integrity").ifPresent(settings::setIntegrity);
        getEnumValue(json, "mirror", Mirror.class).ifPresent(settings::setMirror);
        getBool(json, "ignoreEntities").ifPresent(settings::setIgnoreEntities);

        return settings;
    }

    public static <T extends Enum<T>> Optional<T> getEnumValue(JsonObject json, String field, Class<T> clazz) {
        return getString(json, field).map(s -> getEnumConstant(s, clazz));
    }
}
