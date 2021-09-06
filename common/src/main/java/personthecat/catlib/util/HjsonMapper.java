package personthecat.catlib.util;

import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.FloatRange;
import personthecat.catlib.data.Range;
import personthecat.catlib.exception.JsonMappingException;
import personthecat.fastnoise.data.CellularDistanceType;
import personthecat.fastnoise.data.CellularReturnType;
import personthecat.fastnoise.data.FractalType;
import personthecat.fastnoise.data.NoiseType;
import personthecat.fresult.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static personthecat.catlib.util.Shorthand.map;

/**
 * Generic mapper type which works by consuming a standard {@link JsonObject} and deferring
 * calls to a builder during object construction.
 * <p>
 *     e.g.
 * </p>
 * <pre>{@code
 *
 *   // Declare a reusable mapper.
 *   final HjsonMapper&lt;MyClassBuilder, MyClass&gt; MAPPER =
 *     new HjsonMapper&lt;&gt;("objectName", MyClassBuilder::build)
 *       .mapBool(Fields.enabled, MyClassBuilder::enabled)
 *       .mapInt(Fields.time, MyClassBuilder::time);
 *
 *   // Standard from Json style syntax.
 *   public static MyObject from(final JsonObject json) {
 *     return MAPPER.create(builder(), json);
 *   }
 *
 *   // Handle errors with a Result wrapper.
 *   public static MyObject from2(final JsonObject json) {
 *     return MAPPER.tryCreate(builder(), json)
 *       .ifOk(o -> log.info("You did it!))
 *       .orElseGet(MyObject::new);
 *   }
 *
 *   // Accept backup JSONs in case of missing values.
 *   public static MyObject from3(final JsonObject json, final JsonObject defaults) {
 *       return MAPPER.create(builder(), json, defaults);
 *   }
 *
 * }</pre>
 * <p>
 *   Notice that all default mappers are considered <b>optional</b>. If a given field is
 *   required (i.e. should crash if absent), use the <code>required</code> mappers instead.
 * </p>
 * <p>
 *   e.g. {@link #mapRequiredState}
 * </p>
 */
@AllArgsConstructor
@SuppressWarnings("unused")
public class HjsonMapper<B, R> {

    final List<JsonMapper<B>> mappers = new ArrayList<>();
    final String parent;
    final Function<B, R> buildFunction;

    public HjsonMapper<B, R> mapBool(final String field, final BiConsumer<B, Boolean> ifPresent) {
        return this.add(j -> HjsonUtils.getBool(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredBool(final String field, final BiConsumer<B, Boolean> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getBool(j, field), mapper);
    }

    public HjsonMapper<B, R> mapInt(final String field, final BiConsumer<B, Integer> ifPresent) {
        return this.add(j -> HjsonUtils.getInt(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredInt(final String field, final BiConsumer<B, Integer> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getInt(j, field), mapper);
    }

    public HjsonMapper<B, R> mapIntList(final String field, final BiConsumer<B, IntList> ifPresent) {
        return this.add(j -> HjsonUtils.getIntList(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredIntList(final String field, final BiConsumer<B, IntList> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getIntList(j, field), mapper);
    }

    public HjsonMapper<B, R> mapFloat(final String field, final BiConsumer<B, Float> ifPresent) {
        return this.add(j -> HjsonUtils.getFloat(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredFloat(final String field, final BiConsumer<B, Float> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getFloat(j, field), mapper);
    }

    public HjsonMapper<B, R> mapFloatList(final String field, final BiConsumer<B, FloatList> ifPresent) {
        return this.add(j -> HjsonUtils.getFloatList(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredFloatList(final String field, final BiConsumer<B, FloatList> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getFloatList(j, field), mapper);
    }

    public HjsonMapper<B, R> mapString(final String field, final BiConsumer<B, String> ifPresent) {
        return this.add(j -> HjsonUtils.getString(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredString(final String field, final BiConsumer<B, String> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getString(j, field), mapper);
    }

    public HjsonMapper<B, R> mapStringList(final String field, final BiConsumer<B, List<String>> ifPresent) {
        return this.add(j -> HjsonUtils.getStringArray(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredStringList(final String field, final BiConsumer<B, List<String>> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getStringArray(j, field), mapper);
    }

    public HjsonMapper<B, R> mapId(final String field, final BiConsumer<B, ResourceLocation> ifPresent) {
        return this.add(j -> HjsonUtils.getId(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredId(final String field, final BiConsumer<B, ResourceLocation> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getId(j, field), mapper);
    }

    public HjsonMapper<B, R> mapIds(final String field, final BiConsumer<B, List<ResourceLocation>> ifPresent) {
        return this.add(j -> HjsonUtils.getIds(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredIds(final String field, final BiConsumer<B, List<ResourceLocation>> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getIds(j, field), mapper);
    }

    public HjsonMapper<B, R> mapBiomes(final String field, final BiConsumer<B, List<Biome>> ifPresent) {
        return this.add(j -> HjsonUtils.getBiomeList(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredBiomes(final String field, final BiConsumer<B, List<Biome>> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getBiomeList(j, field), mapper);
    }

    public HjsonMapper<B, R> mapBiomePredicate(final String field, final BiConsumer<B, BiomePredicate> ifPresent) {
        return this.add(j -> HjsonUtils.getBiomePredicate(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredBiomePredicate(final String field, final BiConsumer<B, BiomePredicate> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getBiomePredicate(j, field), mapper);
    }

    public HjsonMapper<B, R> mapRange(final String field, final BiConsumer<B, Range> ifPresent) {
        return this.add(j -> HjsonUtils.getRange(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredRange(final String field, final BiConsumer<B, Range> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getRange(j, field), mapper);
    }

    public HjsonMapper<B, R> mapRangeOrTry(final String field, final String otherField, final BiConsumer<B, Range> ifPresent) {
        final JsonFunction<Range> f = j -> {
            final Optional<Range> range = HjsonUtils.getRange(j, field);
            return range.isPresent() ? range : HjsonUtils.getRange(j, otherField);
        };
        return this.add(f, ifPresent);
    }

    public HjsonMapper<B, R> mapFloatRange(final String field, final BiConsumer<B, FloatRange> ifPresent) {
        return this.add(j -> HjsonUtils.getFloatRange(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredFloatRange(final String field, final BiConsumer<B, FloatRange> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getFloatRange(j, field), mapper);
    }

    public HjsonMapper<B, R> mapDistFunc(final String field, final BiConsumer<B, CellularDistanceType> ifPresent) {
        return mapEnum(field, CellularDistanceType.class, ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredDistFunc(final String field, final BiConsumer<B, CellularDistanceType> mapper) {
        return mapRequiredEnum(field, CellularDistanceType.class, mapper);
    }

    public HjsonMapper<B, R> mapReturnType(final String field, final BiConsumer<B, CellularReturnType> ifPresent) {
        return mapEnum(field, CellularReturnType.class, ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredReturnType(final String field, final BiConsumer<B, CellularReturnType> mapper) {
        return mapRequiredEnum(field, CellularReturnType.class, mapper);
    }

    public HjsonMapper<B, R> mapFractalType(final String field, final BiConsumer<B, FractalType> ifPresent) {
        return mapEnum(field, FractalType.class, ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredFractalType(final String field, final BiConsumer<B, FractalType> mapper) {
        return mapRequiredEnum(field, FractalType.class, mapper);
    }

    public HjsonMapper<B, R> mapNoiseType(final String field, final BiConsumer<B, NoiseType> ifPresent) {
        return mapEnum(field, NoiseType.class, ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredNoiseType(final String field, final BiConsumer<B, NoiseType> mapper) {
        return mapRequiredEnum(field, NoiseType.class, mapper);
    }

    public <E extends Enum<E>> HjsonMapper<B, R> mapEnum(final String field, final Class<E> e, final BiConsumer<B, E> ifPresent) {
        return this.add(j -> HjsonUtils.getEnumValue(j, field, e), ifPresent);
    }

    public <E extends Enum<E>> HjsonMapper<B, R> mapRequiredEnum(final String field, final Class<E> e, final BiConsumer<B, E> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getEnumValue(j, field, e), mapper);
    }

    public <E extends Enum<E>> HjsonMapper<B, R> mapEnumList(final String field, final Class<E> e, final BiConsumer<B, List<E>> ifPresent) {
        return this.add(j -> HjsonUtils.getEnumList(j, field, e), ifPresent);
    }

    public <E extends Enum<E>> HjsonMapper<B, R> mapRequiredEnumList(final String field, final Class<E> e, final BiConsumer<B, List<E>> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getEnumList(j, field, e), mapper);
    }

    public HjsonMapper<B, R> mapState(final String field, final BiConsumer<B, BlockState> ifPresent) {
        return this.add(j -> HjsonUtils.getState(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredState(final String field, final BiConsumer<B, BlockState> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getState(j, field), mapper);
    }

    public HjsonMapper<B, R> mapStateList(final String field, final BiConsumer<B, List<BlockState>> ifPresent) {
        return this.add(j -> HjsonUtils.getStateList(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredStateList(final String field, final BiConsumer<B, List<BlockState>> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getStateList(j, field), mapper);
    }

    public HjsonMapper<B, R> mapBlockPos(final String field, final BiConsumer<B, BlockPos> ifPresent) {
        return this.add(j -> HjsonUtils.getPosition(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredBlockPos(final String field, final BiConsumer<B, BlockPos> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getPosition(j, field), mapper);
    }

    public HjsonMapper<B, R> mapBlockPosList(final String field, final BiConsumer<B, List<BlockPos>> ifPresent) {
        return this.add(j -> HjsonUtils.getPositionList(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredBlockPosList(final String field, final BiConsumer<B, List<BlockPos>> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getPositionList(j, field), mapper);
    }

    public HjsonMapper<B, R> mapPlacementSettings(final BiConsumer<B, StructurePlaceSettings> mapper) {
        return this.add(j -> Optional.of(HjsonUtils.getPlacementSettings(j)), mapper);
    }

    public HjsonMapper<B, R> mapObject(final String field, final BiConsumer<B, JsonObject> ifPresent) {
        return this.add(j -> HjsonUtils.getObject(j, field), ifPresent);
    }

    public HjsonMapper<B, R> mapRequiredObject(final String field, final BiConsumer<B, JsonObject> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getObject(j, field), mapper);
    }

    public <M> HjsonMapper<B, R> mapObject(final String field, final Function<JsonObject, M> f, final BiConsumer<B, M> ifPresent) {
        return this.add(j -> HjsonUtils.getObject(j, field).map(f), ifPresent);
    }

    public <M> HjsonMapper<B, R> mapRequiredObject(final String field, final Function<JsonObject, M> f, final BiConsumer<B, M> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getObject(j, field).map(f), mapper);
    }

    public <M> HjsonMapper<B, R> mapArray(final String field, final Function<JsonObject, M> f, final BiConsumer<B, List<M>> ifPresent) {
        return this.add(this.createArrayGetter(field, f), ifPresent);
    }

    public <M> HjsonMapper<B, R> mapRequiredArray(final String field, final Function<JsonObject, M> f, final BiConsumer<B, List<M>> mapper) {
        return this.addRequired(field, this.createArrayGetter(field, f), mapper);
    }

    private <M> JsonFunction<List<M>> createArrayGetter(final String field, final Function<JsonObject, M> f) {
        return j -> {
            if (j.has(field)) {
                return Optional.of(map(HjsonUtils.getObjectArray(j, field), f));
            }
            return Optional.empty();
        };
    }

    public <M> HjsonMapper<B, R> mapGeneric(final String field, final Function<JsonValue, M> f, final BiConsumer<B, M> ifPresent) {
        return this.add(j -> HjsonUtils.getValue(j, field).map(f), ifPresent);
    }

    public <M> HjsonMapper<B, R> mapRequiredGeneric(final String field, final Function<JsonValue, M> f, final BiConsumer<B, M> mapper) {
        return this.addRequired(field, j -> HjsonUtils.getValue(j, field).map(f), mapper);
    }

    public <M> HjsonMapper<B, R> mapGenericArray(final String field, final Function<JsonValue, M> f, final BiConsumer<B, List<M>> ifPresent) {
        return this.add(this.createListGetter(field, f), ifPresent);
    }

    public <M> HjsonMapper<B, R> mapRequiredGenericArray(final String field, final Function<JsonValue, M> f, final BiConsumer<B, List<M>> mapper) {
        return this.addRequired(field, this.createListGetter(field, f), mapper);
    }

    private <M> JsonFunction<List<M>> createListGetter(final String field, final Function<JsonValue, M> f) {
        return j -> {
            final JsonValue value = j.get(field);
            if (value != null) {
                final List<M> list = new ArrayList<>();
                for (final JsonValue inner : HjsonUtils.asOrToArray(value)) {
                    list.add(f.apply(inner));
                }
                return Optional.of(list);
            }
            return Optional.empty();
        };
    }

    public HjsonMapper<B, R> mapSelf(final BiConsumer<B, JsonObject> mapper) {
        this.mappers.add((j, b) -> {
            mapper.accept(b, j);
            return true;
        });
        return this;
    }

    public <T> HjsonMapper<B, R> add(final JsonFunction<T> getter, final BiConsumer<B, T> ifPresent) {
        this.mappers.add((j, b) -> {
            final Optional<T> t = getter.apply(j);
            t.ifPresent(v -> ifPresent.accept(b, v));
            return t.isPresent();
        });
        return this;
    }

    public <T> HjsonMapper<B, R> addRequired(final String field, final JsonFunction<T> getter, final BiConsumer<B, T> mapper) {
        this.mappers.add((j, b) -> {
            mapper.accept(b, getter.apply(j).orElseThrow(() -> requiredField(field)));
            return true;
        });
        return this;
    }

    /**
     * Applies all of the mappers to a given builder and JSON object(s). Note that
     * an array of backup JSONs may be provided for any <b>optional</b> value.
     *
     * @throws JsonMappingException If any required fields are absent from the primary json.
     * @param builder  The builder object being written into.
     * @param json     The primary source of data for this builder.
     * @param defaults Any additional, backup JSON data sources.
     * @return The main object returned by the builder.
     */
    public R create(final B builder, final JsonObject json, final JsonObject... defaults) {
        for (final JsonMapper<B> f : this.mappers) {
            f.map(json, builder);
        }
        return this.buildFunction.apply(builder);
    }

    /**
     * Variant of {@link HjsonMapper#create} which <b>does not throw</b> any exceptions.
     * Instead, exceptions will be returned in the form of a {@link Result}.
     *
     * @param builder  The builder object being written into.
     * @param json     The primary source of data for this builder.
     * @param defaults Any additional, backup JSON data sources.
     * @return The main object returned by the builder, or else {@link Result#err}.
     */
    public Result<R, JsonMappingException> tryCreate(final B builder, final JsonObject json, final JsonObject... defaults) {
        return Result.<R, JsonMappingException>of(() -> this.create(builder, json, defaults)).ifErr(Result::IGNORE);
    }

    protected JsonMappingException requiredField(final String field) {
        return new JsonMappingException(this.parent, field);
    }

    @FunctionalInterface
    public interface JsonFunction<T> {
        Optional<T> apply(final JsonObject json);
    }

    @FunctionalInterface
    public interface JsonMapper<T> {
        boolean map(final JsonObject json, final T t);
    }
}
