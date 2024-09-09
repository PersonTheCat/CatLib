package personthecat.catlib.config;

import dev.architectury.injectables.annotations.ExpectPlatform;
import lombok.extern.log4j.Log4j2;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.util.unsafe.CachingReflectionHelper;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class ConfigEvaluator {

    public static <T> T getAndRegister(ModDescriptor mod, Class<T> clazz) {
        return getAndRegister(mod, mod.configFile(), clazz);
    }

    public static <T> T getAndRegister(ModDescriptor mod, File file, Class<T> clazz) {
        final T t = CachingReflectionHelper.tryInstantiate(clazz);
        try {
            loadAndRegister(mod, file, clazz, t);
        } catch (final RuntimeException e) {
            LibErrorContext.error(mod, new ConfigLoadException(mod, file.getName(), e));
        }
        return t;
    }

    public static <T> void loadAndRegister(ModDescriptor mod, T t) {
        loadAndRegister(mod, mod.configFile(), t);
    }

    @SuppressWarnings("unchecked")
    public static <T> void loadAndRegister(ModDescriptor mod, File file, T t) {
        loadAndRegister(mod, file, (Class<T>) t.getClass(), t);
    }

    private static <T> void loadAndRegister(ModDescriptor mod, File file, Class<T> clazz, T t) {
        final ConfigValue root = new SimpleValue<>(clazz, file.getName(), t, null);
        root.set(mod, null, t);
        final CategoryValue category = buildCategory(root, t, mod, clazz);
        if (t instanceof Config.Listener c) {
            c.onInstanceCreated(category);
        }
        register(mod, file, category);
    }

    private static CategoryValue buildCategory(
            ConfigValue parent, Object instance, ModDescriptor mod, Class<?> clazz) {
        final List<ConfigValue> values = new ArrayList<>();
        for (final Field f : clazz.getDeclaredFields()) {
            final int m = f.getModifiers();
            if (Modifier.isStatic(m) || Modifier.isTransient(m)) {
                continue;
            }
            final FieldValue value = new FieldValue(mod, f, instance);
            if (ConfigUtil.isLiteralValue(f.getType())) {
                values.add(value);
            } else {
                final Object o = CachingReflectionHelper.tryInstantiate(f.getType());
                values.add(buildCategory(value, o, mod, f.getType()));
                value.set(mod, instance, o);
            }
        }
        return new CategoryValue(parent, values);
    }

    @ExpectPlatform
    private static void register(ModDescriptor mod, File file, CategoryValue config) {}

}
