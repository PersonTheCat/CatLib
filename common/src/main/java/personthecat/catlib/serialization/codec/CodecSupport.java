package personthecat.catlib.serialization.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.experimental.UtilityClass;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.util.ValueLookup;
import personthecat.fresult.Result;
import xjs.data.Json;
import xjs.data.JsonFormat;
import xjs.data.JsonValue;

import java.util.Map;
import java.util.function.Function;

@UtilityClass
public class CodecSupport {

    private static final Map<Class<?>, Function<Object, Codec<?>>> CODECS_BY_TYPE =
        ImmutableMap.<Class<?>, Function<Object, Codec<?>>>builder()
            .put(ConfiguredFeature.class, cf -> ((ConfiguredFeature<?, ?>) cf).feature().configuredCodec().codec())
            .put(ConfiguredWorldCarver.class, cc -> ((ConfiguredWorldCarver<?>) cc).worldCarver().configuredCodec().codec())
            .put(WorldGenSettings.class, s -> WorldGenSettings.CODEC)
            .put(WorldOptions.class, o -> WorldOptions.CODEC.codec())
            .put(WorldDimensions.class, d -> WorldDimensions.CODEC.codec())
            .put(Structure.class, s -> ((Structure) s).type().codec().codec())
            .put(SoundType.class, t -> ValueLookup.SOUND_CODEC)
            .put(MapColor.class, c -> ValueLookup.COLOR_CODEC)
            .put(DensityFunction.class, f -> ((DensityFunction) f).codec().codec().codec())
            .build();

    /**
     * Stringifies the given object <em>if</em> it has a discernible {@link
     * Codec}
     *
     * @param o The object being stringified.
     * @return A string representation of the value.
     */
    public static @Nullable String anyToString(final Object o) {
        if (o == null) return "null";
        final JsonValue value = serializeAny(o);
        if (value == null) return null;
        return value.toString(JsonFormat.DJS_FORMATTED);
    }

    /**
     * Converts the given object to a {@link JsonValue} <em>if</em> it has
     * as discernible {@link Codec}. Else, returns null.
     *
     * @param o The object being serialized.
     * @return A {@link JsonValue json} representation of the value.
     */
    public static @Nullable JsonValue serializeAny(final Object o) {
        if (o == null) return Json.value(null);
        final Codec<Object> c = tryGetCodec(o);
        if (c == null) return null;
        return c.encodeStart(XjsOps.INSTANCE, o).result().orElse(null);
    }

    /**
     * Attempts to resolve the {@link Codec} of the given value reflectively.
     *
     * Some edge cases may support non-reflective access.
     *
     * @param t   The object being inspected.
     * @param <T> The type of object being inspected.
     * @return The {@link Codec codec}, else <code>null</code>.
     */
    public static <T> @Nullable Codec<T> tryGetCodec(final T t) {
        if (t == null) return null;
        final Function<Object, Codec<?>> c = CODECS_BY_TYPE.get(t.getClass());
        if (c != null) return cast(c.apply(t));
        return cast(getReflectively(t));
    }

    // mostly for custom content / dev. doesn't work for vanilla features due to remapping
    private static @Nullable Codec<?> getReflectively(final Object o) {
        Codec<?> c;
        if ((c = getCodecAtPath(o, "codec")) != null) return c;
        if ((c = getCodecAtPath(o, "type", "codec")) != null) return c;
        if ((c = getCodecAtPath(o, "CODEC")) != null) return c;
        return null;
    }

    private static @Nullable Codec<?> getCodecAtPath(final Object o, final String... path) {
        Object p = o;
        Object v = null;
        for (final String s : path) {
            final Object parent = p;
            v = Result.suppress(() -> parent.getClass().getDeclaredField(s))
                .andThenTry(f -> f.get(parent))
                .orElseGet(e -> null);
            if (v == null) return null;
            p = v;
        }
        return fieldAsCodec(v);
    }

    private static @Nullable Codec<?> fieldAsCodec(final Object o) {
        if (o instanceof MapCodec<?> m) return m.codec();
        if (o instanceof Codec<?> c) return c;
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> Codec<T> cast(final Codec<?> codec) {
        return (Codec<T>) codec;
    }
}
