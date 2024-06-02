package personthecat.catlib.config;

import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;

import java.util.List;

public record CategoryValue(ConfigValue parent, List<ConfigValue> values) implements ConfigValue {

    @Override
    public Class<?> type() {
        return this.parent.type();
    }

    @Override
    public String name() {
        return this.parent.name();
    }

    @Override
    public void set(ModDescriptor mod, Object instance, Object value) {
        this.parent.set(mod, instance, value);
    }

    @Override
    public @Nullable Object get(ModDescriptor mod, Object instance) {
        return this.parent.get(mod, instance);
    }

    @Override
    public @Nullable String comment() {
        return this.parent.comment();
    }

    @Override
    public @Nullable Object defaultValue() {
        return this.parent.defaultValue();
    }

    @Override
    public List<Validation<?>> validations() {
        return this.parent.validations();
    }
}
