package personthecat.catlib.config;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.FormattedException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class FieldValue implements ConfigValue {
    private static final String CONFIG_CATEGORY = "catlib.errorMenu.config";
    private static final String GENERIC_ERROR = "catlib.errorText.configField";
    private final Field field;
    private final Object defaultValue;
    private final List<Validation<?>> validations;

    public FieldValue(ModDescriptor mod, Field field, Object instance) {
        this.field = field;

        this.validations = new ArrayList<>();
        try {
            this.field.setAccessible(true);
        } catch (final RuntimeException e) {
            LibErrorContext.error(mod, new FieldException("Error setting accessible", e, this.field));
        }
        this.defaultValue = this.get(mod, instance);
        loadValidations(this.validations, field);
    }

    private static void loadValidations(List<Validation<?>> validations, Field field) {
        addValidation(validations, field, Config.GenericType.class, Validation::from);
        addValidation(validations, field, Config.Range.class, Validation::from);
        addValidation(validations, field, Config.DecimalRange.class, Validation::from);
        addValidation(validations, field, Config.Regex.class, Validation::from);
        addValidation(validations, field, Config.NotBlank.class, Validation::from);
        addValidation(validations, field, Config.NotNull.class, Validation::from);
    }

    private static <T extends Annotation, C extends Class<T>> void addValidation(
            List<Validation<?>> validations, Field field, C c, Function<T, Validation<?>> f) {
        if (field.isAnnotationPresent(c)) {
            validations.add(f.apply(field.getAnnotation(c)));
        }
    }

    public Field getField() {
        return this.field;
    }

    @Override
    public Class<?> type() {
        return ConfigUtil.toBoxedType(this.field.getType());
    }

    @Override
    public String name() {
        return this.field.getName();
    }

    @Override
    public void set(ModDescriptor mod, Object instance, Object value) {
        try {
            this.field.set(instance, value);
        } catch (final ReflectiveOperationException e) {
            LibErrorContext.error(mod, new FieldException("Error setting value: " + value, e, this.field));
        }
    }

    @Override
    public Object get(ModDescriptor mod, Object instance) {
        try {
            return this.field.get(instance);
        } catch (final ReflectiveOperationException e) {
            LibErrorContext.error(mod, new FieldException("Error getting value", e, this.field));
            return null;
        }
    }

    @Override
    public String comment() {
        if (!this.field.isAnnotationPresent(Config.Comment.class)) {
            return null;
        }
        return this.field.getAnnotation(Config.Comment.class).value();
    }

    @Override
    public @Nullable Object defaultValue() {
        return this.defaultValue;
    }

    @Override
    public List<Validation<?>> validations() {
        return this.validations;
    }

    @Override
    public boolean needsWorldRestart() {
        return this.field.isAnnotationPresent(Config.NeedsWorldRestart.class);
    }

    @Override
    public boolean canBeNull() {
        return this.defaultValue == null || this.field.isAnnotationPresent(Config.CanBeNull.class);
    }

    @Override
    public String toString() {
        return this.field.toString();
    }

    private static class FieldException extends FormattedException {
        final Field field;

        FieldException(String msg, Throwable cause, Field field) {
            super(msg, cause);
            this.field = field;
        }

        @Override
        public @NotNull String getCategory() {
            return CONFIG_CATEGORY;
        }

        @Override
        public @NotNull Component getDisplayMessage() {
            return Component.literal(this.getFullMethod());
        }

        @Override
        public @Nullable Component getTooltip() {
            return Component.translatable(this.getMessage(), this.field.getName());
        }

        @Override
        public @NotNull Component getTitleMessage() {
            return Component.translatable(GENERIC_ERROR, this.getFullMethod());
        }

        private String getFullMethod() {
            return this.field.getDeclaringClass().getSimpleName() + "." + this.field.getName();
        }
    }
}
