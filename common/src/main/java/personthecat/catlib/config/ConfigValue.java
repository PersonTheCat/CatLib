package personthecat.catlib.config;

import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;

import java.util.List;

public interface ConfigValue {
    Class<?> type();
    String name();
    void set(ModDescriptor mod, Object instance, Object value);
    @Nullable Object get(ModDescriptor mod, Object instance);
    @Nullable String comment();
    @Nullable Object defaultValue();
    default List<Validation<?>> validations() { return List.of(); }
    default boolean needsWorldRestart() { return false; }
    default boolean canBeNull() { return false; }
}