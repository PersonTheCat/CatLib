package personthecat.catlib.util.unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static personthecat.catlib.util.Shorthand.f;

public class CachingReflectionHelper {

    private static final Map<Class<?>, Object> INSTANCE_MAP = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T tryInstantiate(final Class<T> c) {
        return (T) INSTANCE_MAP.computeIfAbsent(c, k -> constructWithoutArgs(c));
    }

    private static <T> T constructWithoutArgs(final Class<T> c) {
        for (final Constructor<?> constructor : c.getConstructors()) {
            if (constructor.getParameterCount() == 0) {
                constructor.setAccessible(true);
                return tryInvoke(c, constructor);
            }
        }
        throw new UncheckedReflectiveAccessException("Missing no arg constructor: ", c);
    }

    @SuppressWarnings("unchecked")
    private static <T> T tryInvoke(final Class<T> c, final Constructor<?> constructor) {
        try {
            return (T) constructor.newInstance();
        } catch (final IllegalAccessException | InvocationTargetException | InstantiationException ignored) {
            throw new UncheckedReflectiveAccessException("Could not instantiate: {}", c.getSimpleName());
        }
    }

    private static class UncheckedReflectiveAccessException extends RuntimeException {
        UncheckedReflectiveAccessException(final String s, final Object... args) {
            super(f(s, args));
        }
    }
}
