package personthecat.catlib.config;

import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;

import java.util.List;

public class SimpleValue<T> implements ConfigValue {
    private final @Nullable String comment;
    private final Class<T> type;
    private final String name;
    private final List<Validation<?>> validations;
    private final T defaultValue;
    private T value;

    public SimpleValue(
            Class<T> type, String name, @Nullable T def, @Nullable String comment) {
        this.comment = comment;
        this.type = type;
        this.name = name;
        this.validations = List.of(new Validation.Typed(type));
        this.defaultValue = def;
        this.value = def;
    }

    @Override
    public Class<?> type() {
        return this.type;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(ModDescriptor mod, Object instance, Object value) {
        this.value = (T) value;
    }

    @Override
    public @Nullable Object get(ModDescriptor mod, Object instance) {
        return this.value;
    }

    @Override
    public @Nullable String comment() {
        return this.comment;
    }

    @Override
    public @Nullable Object defaultValue() {
        return this.defaultValue;
    }

    @Override
    public List<Validation<?>> validations() {
        return this.validations;
    }
}
