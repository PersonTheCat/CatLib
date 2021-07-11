package personthecat.catlib.util;


import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.JsonMappingException;
import personthecat.fastnoise.FastNoise.CellularDistanceFunction;
import personthecat.fastnoise.FastNoise.CellularReturnType;
import personthecat.fastnoise.FastNoise.FractalType;
import personthecat.fastnoise.FastNoise.Interp;
import personthecat.fastnoise.FastNoise.NoiseType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static personthecat.catlib.exception.Exceptions.mappingEx;
import static personthecat.catlib.util.Shorthand.map;

/**
 * Generic mapper type which works by consuming a standard {@link JsonObject} and deferring
 * calls to a builder during object construction.
 * <p>
 *     e.g.
 * </p>
 * <pre>
 *   // Standard from Json style syntax.
 *   public static MyObject from(final JsonObject json) {
 *     final MyObjectBuilder builder = builder();
 *     return new HjsonMapper(OBJECT_NAME, json)
 *       .mapBool(Fields.enabled, builder::enabled)
 *       .mapInt(Fields.time, builder::time)
 *       .release(builder::build);
 *   }
 * </pre>
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
public class HjsonMapper {

    private final String parent;
    private final JsonObject json;

    public HjsonMapper mapBool(final String field, final Consumer<Boolean> ifPresent) {
        HjsonTools.getBool(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredBool(final String field, final Consumer<Boolean> mapper) {
        mapper.accept(HjsonTools.getBool(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapInt(final String field, final Consumer<Integer> ifPresent) {
        HjsonTools.getInt(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredInt(final String field, final Consumer<Integer> mapper) {
        mapper.accept(HjsonTools.getInt(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapIntList(final String field, final Consumer<List<Integer>> ifPresent) {
        HjsonTools.getIntList(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredIntList(final String field, final Consumer<List<Integer>> mapper) {
        mapper.accept(HjsonTools.getIntList(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapFloat(final String field, final Consumer<Float> ifPresent) {
        HjsonTools.getFloat(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredFloat(final String field, final Consumer<Float> mapper) {
        mapper.accept(HjsonTools.getFloat(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapString(final String field, final Consumer<String> ifPresent) {
        HjsonTools.getString(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredString(final String field, final Consumer<String> mapper) {
        mapper.accept(HjsonTools.getString(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapBiomes(final String field, final Consumer<List<Biome>> ifPresent) {
        HjsonTools.getBiomeList(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredBiomes(final String field, final Consumer<List<Biome>> mapper) {
        mapper.accept(HjsonTools.getBiomeList(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapRange(final String field, final Consumer<Range> ifPresent) {
        HjsonTools.getRange(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredRange(final String field, final Consumer<Range> mapper) {
        mapper.accept(HjsonTools.getRange(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapRangeOrTry(final String field, final String otherField, final Consumer<Range> ifPresent) {
        final Optional<Range> range = HjsonTools.getRange(json, field);
        range.ifPresent(ifPresent);
        if (!range.isPresent()) {
            return mapRange(otherField, ifPresent);
        }
        return this;
    }

    public HjsonMapper mapFloatRange(final String field, final Consumer<FloatRange> ifPresent) {
        HjsonTools.getFloatRange(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredFloatRange(final String field, final Consumer<FloatRange> mapper) {
        mapper.accept(HjsonTools.getFloatRange(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapDistFunc(final String field, final Consumer<CellularDistanceFunction> ifPresent) {
        return mapEnum(field, CellularDistanceFunction.class, ifPresent);
    }

    public HjsonMapper mapRequiredDistFunc(final String field, final Consumer<CellularDistanceFunction> mapper) {
        return mapRequiredEnum(field, CellularDistanceFunction.class, mapper);
    }

    public HjsonMapper mapReturnType(final String field, final Consumer<CellularReturnType> ifPresent) {
        return mapEnum(field, CellularReturnType.class, ifPresent);
    }

    public HjsonMapper mapRequiredReturnType(final String field, final Consumer<CellularReturnType> mapper) {
        return mapRequiredEnum(field, CellularReturnType.class, mapper);
    }

    public HjsonMapper mapFractalType(final String field, final Consumer<FractalType> ifPresent) {
        return mapEnum(field, FractalType.class, ifPresent);
    }

    public HjsonMapper mapRequiredFractalType(final String field, final Consumer<FractalType> mapper) {
        return mapRequiredEnum(field, FractalType.class, mapper);
    }

    public HjsonMapper mapInterp(final String field, final Consumer<Interp> ifPresent) {
        return mapEnum(field, Interp.class, ifPresent);
    }

    public HjsonMapper mapRequiredInterp(final String field, final Consumer<Interp> mapper) {
        return mapRequiredEnum(field, Interp.class, mapper);
    }

    public HjsonMapper mapNoiseType(final String field, final Consumer<NoiseType> ifPresent) {
        return mapEnum(field, NoiseType.class, ifPresent);
    }

    public HjsonMapper mapRequiredNoiseType(final String field, final Consumer<NoiseType> mapper) {
        return mapRequiredEnum(field, NoiseType.class, mapper);
    }

    public <E extends Enum<E>> HjsonMapper mapEnum(final String field, final Class<E> e, final Consumer<E> ifPresent) {
        HjsonTools.getEnumValue(json, field, e).ifPresent(ifPresent);
        return this;
    }

    public <E extends Enum<E>> HjsonMapper mapRequiredEnum(final String field, final Class<E> e, final Consumer<E> mapper) {
        mapper.accept(HjsonTools.getEnumValue(json, field, e).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapState(final String field, final Consumer<BlockState> ifPresent) {
        HjsonTools.getState(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredState(final String field, final Consumer<BlockState> mapper) {
        mapper.accept(HjsonTools.getState(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapStateList(final String field, final Consumer<List<BlockState>> ifPresent) {
        HjsonTools.getStateList(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredStateList(final String field, final Consumer<List<BlockState>> mapper) {
        mapper.accept(HjsonTools.getStateList(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapBlockPos(final String field, final Consumer<BlockPos> ifPresent) {
        HjsonTools.getPosition(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredBlockPos(final String field, final Consumer<BlockPos> mapper) {
        mapper.accept(HjsonTools.getPosition(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapBlockPosList(final String field, final Consumer<List<BlockPos>> ifPresent) {
        HjsonTools.getPositionList(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredBlockPosList(final String field, final Consumer<List<BlockPos>> mapper) {
        mapper.accept(HjsonTools.getPositionList(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public HjsonMapper mapPlacementSettings(final Consumer<StructureBlockEntity> mapper) {
        mapper.accept(HjsonTools.getPlacementSettings(json));
        return this;
    }

    public HjsonMapper mapObject(final String field, final Consumer<JsonObject> ifPresent) {
        HjsonTools.getObject(json, field).ifPresent(ifPresent);
        return this;
    }

    public HjsonMapper mapRequiredObject(final String field, final Consumer<JsonObject> mapper) {
        mapper.accept(HjsonTools.getObject(json, field).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public <M> HjsonMapper mapObject(final String field, final Function<JsonObject, M> f, final Consumer<M> ifPresent) {
        HjsonTools.getObject(json, field).map(f).ifPresent(ifPresent);
        return this;
    }

    public <M> HjsonMapper mapRequiredObject(final String field, final Function<JsonObject, M> f, final Consumer<M> mapper) {
        mapper.accept(HjsonTools.getObject(json, field).map(f).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public <M> HjsonMapper mapArray(final String field, final Function<JsonObject, M> f, final Consumer<List<M>> ifPresent) {
        if (json.has(field)) {
            ifPresent.accept(map(HjsonTools.getObjectArray(json, field), f));
        }
        return this;
    }

    public <M> HjsonMapper mapRequiredArray(final String field, final Function<JsonObject, M> f, final Consumer<List<M>> mapper) {
        if (!json.has(field)) {
            throw requiredField(field);
        }
        mapper.accept(map(HjsonTools.getObjectArray(json, field), f));
        return this;
    }

    public <M> HjsonMapper mapGeneric(final String field, final Function<JsonValue, M> f, final Consumer<M> ifPresent) {
        HjsonTools.getValue(json, field).map(f).ifPresent(ifPresent);
        return this;
    }

    public <M> HjsonMapper mapRequiredGeneric(final String field, final Function<JsonValue, M> f, final Consumer<M> mapper) {
        mapper.accept(HjsonTools.getValue(json, field).map(f).orElseThrow(() -> requiredField(field)));
        return this;
    }

    public <M> HjsonMapper mapGenericArray(final String field, final Function<JsonValue, M> f, final Consumer<List<M>> ifPresent) {
        final JsonValue value = json.get(field);
        if (value != null) {
            mapGenericArrayInternal(value, f, ifPresent);
        }
        return this;
    }

    public <M> HjsonMapper mapRequiredGenericArray(final String field, final Function<JsonValue, M> f, final Consumer<List<M>> mapper) {
        final JsonValue value = json.get(field);
        if (value == null) {
            throw requiredField(field);
        }
        mapGenericArrayInternal(value, f, mapper);
        return this;
    }

    private <M> void mapGenericArrayInternal(@NotNull JsonValue value, final Function<JsonValue, M> f, final Consumer<List<M>> mapper) {
        final List<M> list = new ArrayList<>();
        for (final JsonValue inner : HjsonTools.asOrToArray(value)) {
            list.add(f.apply(inner));
        }
        mapper.accept(list);
    }

    public HjsonMapper mapSelf(final Consumer<JsonObject> mapper) {
        mapper.accept(this.json);
        return this;
    }

    public <T> T release(final Supplier<T> supplier) {
        return supplier.get();
    }

    protected JsonMappingException requiredField(final String field) {
        return mappingEx("{}.{} is required", this.parent, field);
    }
}
