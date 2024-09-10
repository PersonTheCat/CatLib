package personthecat.catlib.serialization.codec;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.registry.RegistryHandle;
import personthecat.catlib.registry.RegistryUtils;
import xjs.data.Json;
import xjs.data.JsonFormat;
import xjs.data.JsonValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class CodecSupport {

    private static final Map<Class<?>, Function<Object, Codec<?>>> CODECS_BY_TYPE = new ConcurrentHashMap<>();

    private CodecSupport() {}

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
            final Codec<?> codec = getFromDataLoader(t);
            return codec != null ? o -> codec : null;
        });
        return c != null ? cast(c.apply(t)) : null;
    }

    @SuppressWarnings("DataFlowIssue") // definitely does return null
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

    @SuppressWarnings("unchecked")
    private static <T> Codec<T> cast(final Codec<?> codec) {
        return (Codec<T>) codec;
    }
}
