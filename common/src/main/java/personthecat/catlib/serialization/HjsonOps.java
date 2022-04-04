package personthecat.catlib.serialization;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class HjsonOps implements DynamicOps<JsonValue> {

    public static final HjsonOps INSTANCE = new HjsonOps(false);
    public static final HjsonOps COMPRESSED = new HjsonOps(true);

    private static final JsonValue EMPTY = JsonValue.valueOf(null);

    private final boolean compressed;

    private HjsonOps(final boolean compressed) {
        this.compressed = compressed;
    }

    @Override
    public JsonValue empty() {
        return EMPTY;
    }

    @Override
    public <U> U convertTo(final DynamicOps<U> outOps, final JsonValue input) {
        if (input == null || input.isNull()) {
          return outOps.empty();
        } else if (input.isObject()) {
            return this.convertMap(outOps, input);
        } else if (input.isArray()) {
            return this.convertList(outOps, input);
        } else if (input.isString()) {
            return outOps.createString(input.asString());
        } else if (input.isBoolean()) {
            return outOps.createBoolean(input.asBoolean());
        } else if (input.isNumber()) {
            return this.toNumber(outOps, input.asDouble());
        }
        return null;
    }

    private <U> U toNumber(final DynamicOps<U> outOps, final double number) {
        if ((byte) number == number) {
            return outOps.createByte((byte) number);
        } else if ((short) number == number) {
            return outOps.createShort((short) number);
        } else if ((int) number == number) {
            return outOps.createInt((int) number);
        } else if ((float) number == number) {
            return outOps.createFloat((float) number);
        }
        return outOps.createDouble(number);
    }

    @Override
    public DataResult<Number> getNumberValue(final JsonValue input) {
        if (input == null || input.isNull()) {
            return DataResult.error("Not a number: null");
        } else if (input.isNumber()) {
            return DataResult.success(input.asDouble());
        } else if (input.isBoolean()) {
            return DataResult.success(input.asBoolean() ? 1 : 0);
        }
        if (this.compressed && input.isString()) {
            try {
                return DataResult.success(Integer.parseInt(input.asString()));
            } catch (final NumberFormatException e) {
                return DataResult.error("Not a number: " + e + " " + input);
            }
        }
        return DataResult.error("Not a number: " + input);
    }

    @Override
    public JsonValue createNumeric(final Number i) {
        return JsonValue.valueOf(i.doubleValue());
    }

    @Override
    public DataResult<Boolean> getBooleanValue(final JsonValue input) {
        if (input == null || input.isNull()) {
            return DataResult.error("Not a boolean: null");
        } else if (input.isBoolean()) {
            return DataResult.success(input.asBoolean());
        } else if (input.isNumber()) {
            return DataResult.success(input.asDouble() != 0);
        }
        return DataResult.error("Not a boolean: " + input);
    }

    @Override
    public JsonValue createBoolean(final boolean value) {
        return JsonValue.valueOf(value);
    }

    @Override
    public DataResult<String> getStringValue(final JsonValue input) {
        if (input == null || input.isNull()) {
            return DataResult.error("Not a string: null");
        } else if (input.isString()) {
            return DataResult.success(input.asString());
        } else if (this.compressed && input.isNumber()) {
            return DataResult.success(String.valueOf(input.asDouble()));
        }
        return DataResult.error("Not a string: " + input);
    }

    @Override
    public JsonValue createString(final String value) {
        return JsonValue.valueOf(value);
    }

    @Override
    public DataResult<JsonValue> mergeToList(final JsonValue list, final JsonValue value) {
        if (list == null || list.isNull()) {
            return DataResult.success(new JsonArray().add(value));
        } else if (list.isArray()) {
            return DataResult.success(new JsonArray().addAll(list.asArray()).add(value));
        }
        return DataResult.error("mergeToList called with not a list: " + list, list);
    }

    @Override
    public DataResult<JsonValue> mergeToList(final JsonValue list, final List<JsonValue> values) {
        if (list == null || list.isNull()) {
            final JsonArray result = new JsonArray();
            values.forEach(result::add);
            return DataResult.success(result);
        } else if (list.isArray()) {
            final JsonArray result = new JsonArray(list.asArray());
            values.forEach(result::add);
            return DataResult.success(result);
        }
        return DataResult.error("mergeToList called with not a list: " + list, list);
    }

    @Override
    public DataResult<JsonValue> mergeToMap(final JsonValue map, final JsonValue key, final JsonValue value) {
        if (!(map == null || map.isObject() || map.isNull())) {
            return DataResult.error("mergeToMap called with not a map: " + map, map);
        } else if (!(key.isString() || (this.compressed && isPrimitiveLike(key)))) {
            final String msg = "key is not a string: " + key;
            return map != null ? DataResult.error(msg, map) : DataResult.error(msg);
        }
        if (map == null || map.isNull()) {
            return DataResult.success(new JsonObject().add(asPrimitiveString(key), value));
        }
        return DataResult.success(new JsonObject().addAll(map.asObject()).add(asPrimitiveString(key), value));
    }

    @Override
    public DataResult<JsonValue> mergeToMap(final JsonValue map, MapLike<JsonValue> values) {
        if (!(map == null || map.isObject() || map.isNull())) {
            return DataResult.error("mergeToMap called with not a map: " + map, map);
        }
        final JsonObject output = new JsonObject();
        if (map != null && map.isObject()) {
            output.addAll(map.asObject());
        }
        final List<JsonValue> missed = new ArrayList<>();
        values.entries().forEach(entry -> {
            final JsonValue key = entry.getFirst();
            if (key.isString() || (this.compressed && isPrimitiveLike(key))) {
                output.add(asPrimitiveString(key), entry.getSecond());
            } else {
                missed.add(key);
            }
        });
        if (!missed.isEmpty()) {
            return DataResult.error("some keys are not strings: " + missed, output);
        }
        return DataResult.success(output);
    }

    @Override
    public DataResult<Stream<Pair<JsonValue, JsonValue>>> getMapValues(final JsonValue input) {
        if (input == null || !input.isObject()) {
            return DataResult.error("Not an Hjson object: " + input);
        }
        final Stream.Builder<Pair<JsonValue, JsonValue>> builder = Stream.builder();
        for (final JsonObject.Member member : input.asObject()) {
            final JsonValue value = member.getValue();
            builder.add(Pair.of(JsonValue.valueOf(member.getName()), value.isNull() ? null : value));
        }
        return DataResult.success(builder.build());
    }

    @Override
    public DataResult<Consumer<BiConsumer<JsonValue, JsonValue>>> getMapEntries(final JsonValue input) {
        if (input == null || !input.isObject()) {
            return DataResult.error("Not an Hjson object: " + input);
        }
        return DataResult.success(c -> {
            for (final JsonObject.Member member : input.asObject()) {
                final JsonValue value = member.getValue();
                c.accept(JsonValue.valueOf(member.getName()), value.isNull() ? null : value);
            }
        });
    }

    @Override
    public DataResult<MapLike<JsonValue>> getMap(final JsonValue input) {
        if (input == null || !input.isObject()) {
            return DataResult.error("Not an Hjson object: " + input);
        }
        return DataResult.success(new HjsonMapLike(input.asObject()));
    }

    @Override
    public JsonValue createMap(final Stream<Pair<JsonValue, JsonValue>> map) {
        final JsonObject result = new JsonObject();
        map.forEach(p -> {
            final JsonValue v = p.getSecond();
            result.add(p.getFirst().asString(), v != null ? v : EMPTY);
        });
        return result;
    }

    @Override
    public DataResult<Stream<JsonValue>> getStream(final JsonValue input) {
        if (input == null || !input.isArray()) {
            return DataResult.error("Not an Hjson array: " + input);
        }
        final Stream.Builder<JsonValue> builder = Stream.builder();
        for (final JsonValue value : input.asArray()) {
            builder.add(value.isNull() ? null : value);
        }
        return DataResult.success(builder.build());
    }

    @Override
    public DataResult<Consumer<Consumer<JsonValue>>> getList(final JsonValue input) {
        if (input == null || !input.isArray()) {
            return DataResult.error("Not an Hjson array: + " + input);
        }
        return DataResult.success(c -> {
            for (final JsonValue value : input.asArray()) {
                c.accept(value.isNull() ? null : value);
            }
        });
    }

    @Override
    public JsonValue createList(final Stream<JsonValue> input) {
        final JsonArray result = new JsonArray();
        input.forEach(v -> result.add(v != null ? v : EMPTY));
        return result;
    }

    @Override
    public JsonValue remove(final JsonValue input, final String key) {
        if (input != null && input.isObject()) {
            final JsonObject result = new JsonObject();
            for (final JsonObject.Member member : input.asObject()) {
                if (!member.getName().equals(key)) {
                    result.add(member.getName(), member.getValue());
                }
            }
            return result;
        }
        return input;
    }

    @Override
    public boolean compressMaps() {
        return this.compressed;
    }

    @Override
    public String toString() {
        return "Hjson";
    }

    private record HjsonMapLike(JsonObject object) implements MapLike<JsonValue> {

        @Nullable
        @Override
        public JsonValue get(final JsonValue key) {
            final JsonValue value = this.object.get(key.asString());
            return value != null && value.isNull() ? null : value;
        }

        @Nullable
        @Override
        public JsonValue get(final String key) {
            final JsonValue value = this.object.get(key);
            return value != null && value.isNull() ? null : value;
        }

        @Override
        public Stream<Pair<JsonValue, JsonValue>> entries() {
            final Stream.Builder<Pair<JsonValue, JsonValue>> builder = Stream.builder();
            for (final JsonObject.Member member : this.object) {
                final JsonValue value = member.getValue();
                builder.add(Pair.of(JsonValue.valueOf(member.getName()), value.isNull() ? null : value));
            }
            return builder.build();
        }

        @Override
        public String toString() {
            return "HjsonMapLike[" + this.object + "]";
        }
    }

    private static boolean isPrimitiveLike(final JsonValue value) {
        return value.isBoolean() || value.isString() || value.isNumber();
    }

    private static String asPrimitiveString(final JsonValue value) {
        return value.isNumber() ? String.valueOf(value.asDouble()) : value.asString();
    }
}
