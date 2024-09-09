package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.experimental.UtilityClass;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
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
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.registry.RegistryUtils;
import personthecat.catlib.util.ValueLookup;
import personthecat.fresult.Result;
import xjs.data.Json;
import xjs.data.JsonFormat;
import xjs.data.JsonValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@UtilityClass
public class CodecSupport {

    private static final Map<Class<?>, Function<Object, Codec<?>>> CODECS_BY_TYPE = new ConcurrentHashMap<>();

    static {
        registerGetter(ConfiguredFeature.class, cf -> cf.feature().configuredCodec().codec());
        registerGetter(ConfiguredWorldCarver.class, cc -> cc.worldCarver().configuredCodec().codec());
        registerGetter(WorldGenSettings.class, s -> WorldGenSettings.CODEC);
        registerGetter(WorldOptions.class, o -> WorldOptions.CODEC.codec());
        registerGetter(WorldDimensions.class, d -> WorldDimensions.CODEC.codec());
        registerGetter(Structure.class, s -> s.type().codec().codec());
        registerGetter(SoundType.class, t -> ValueLookup.SOUND_CODEC);
        registerGetter(MapColor.class, c -> ValueLookup.COLOR_CODEC);
        registerGetter(DensityFunction.class, f -> f.codec().codec().codec());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> void registerGetter(final Class<T> clazz, final Function<? extends T, Codec<?>> getter) {
        CODECS_BY_TYPE.put(clazz, (Function) getter);
    }

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
     * <p>
     * Some edge cases may support non-reflective access.
     *
     * @param t   The object being inspected.
     * @param <T> The type of object being inspected.
     * @return The {@link Codec codec}, else <code>null</code>.
     */
    public static <T> @Nullable Codec<T> tryGetCodec(final T t) {
        if (t == null) return null;
        final Function<Object, Codec<?>> c = CODECS_BY_TYPE.computeIfAbsent(t.getClass(), clazz -> {
            final Codec<?> codec = resolveCodec(t);
            return codec != null ? o -> codec : null;
        });
        return c != null ? cast(c.apply(t)) : null;
    }

    private static @Nullable Codec<?> resolveCodec(final Object t) {
        final Codec<?> c = getFromDataLoader(t);
        if (c != null) return c;
        return getReflectively(t);
    }

    private static @Nullable Codec<?> getFromDataLoader(final Object t) {
        final ResourceKey<? extends Registry<?>> key =
            RegistryUtils.tryGetByType(t.getClass()).map(RegistryHandle::key).orElse(null);
        if (key == null) {
            return null;
        }
        Codec<?> c = lookup(RegistryDataLoader.WORLDGEN_REGISTRIES, key);
        if (c != null) return c;
        c = lookup(RegistryDataLoader.DIMENSION_REGISTRIES, key);
        if (c != null) return c;
        return lookup(RegistryDataLoader.SYNCHRONIZED_REGISTRIES, key);
    }

    private static @Nullable Codec<?> lookup(
            final List<RegistryDataLoader.RegistryData<?>> registries, ResourceKey<? extends Registry<?>> key) {
        for (final RegistryDataLoader.RegistryData<?> data : registries) {
            if (data.key().equals(key)) {
                return data.elementCodec();
            }
        }
        return null;
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
