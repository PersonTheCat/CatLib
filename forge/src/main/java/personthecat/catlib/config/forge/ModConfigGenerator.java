package personthecat.catlib.config.forge;

import com.electronwill.nightconfig.core.EnumGetMethod;
import lombok.extern.log4j.Log4j2;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import personthecat.catlib.config.CategoryValue;
import personthecat.catlib.config.Config;
import personthecat.catlib.config.ConfigUtil;
import personthecat.catlib.config.ConfigValue;
import personthecat.catlib.config.Validation;
import personthecat.catlib.config.Validation.DecimalRange;
import personthecat.catlib.config.Validation.GenericTyped;
import personthecat.catlib.config.Validation.Range;
import personthecat.catlib.config.Validation.Typed;
import personthecat.catlib.config.ValidationException;
import personthecat.catlib.config.ValidationMap;
import personthecat.catlib.config.Validations;
import personthecat.catlib.config.ValueException;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.FormattedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Log4j2
@SuppressWarnings({"unchecked", "rawtypes"})
public class ModConfigGenerator {
    private final ModDescriptor mod;
    private final CategoryValue config;
    private final Object instance;
    private final ValidationMap validations;

    public ModConfigGenerator(ModDescriptor mod, CategoryValue config) {
        this.mod = mod;
        this.config = config;
        this.instance = config.parent().get(mod, null);
        this.validations = new ValidationMap();
    }

    public ForgeConfigSpec generateSpec() {
        final Builder builder = new Builder();
        for (final ConfigValue v : config.values()) {
            this.apply(builder, v);
        }
        return builder.build();
    }

    public void fireOnConfigUpdated() {
        if (this.instance instanceof Config.Listener c) {
            try {
                c.onConfigUpdated();
            } catch (final ValidationException e) {
                LibErrorContext.error(this.mod, e);
            }
        }
    }

    public void updateConfig(ForgeConfigSpec spec) {
        this.updateAll(spec, List.of(), this.instance, this.config);
        this.fireOnConfigUpdated();
    }

    private void apply(Builder builder, ConfigValue value) {
        final String comment = value.comment();
        final String name = value.name();
        final Object def = value.defaultValue();
        if (comment != null) {
            builder.comment(comment);
        }
        if (value.needsWorldRestart()) {
            builder.worldRestart();
        }
        if (value instanceof CategoryValue category) {
            builder.push(name);
            for (final ConfigValue v : category.values()) {
                this.apply(builder, v);
            }
            builder.pop();
            return;
        }
        final Validations validations;
        try {
            validations = Validations.fromValue(this.filename(), value);
        } catch (final ValueException e) {
            this.error(e);
            return;
        }
        this.validations.put(value, validations);
        if (value.type().isEnum()) {
            this.defineEnum(builder, value, def, validations);
        } else if (value.type().isArray()) {
            this.defineArray(builder, value, def, validations);
        } else if (def == null || (!this.defineBoolean(builder, value, def)
                && !this.defineInt(builder, value, def, validations)
                && !this.defineLong(builder, value, def, validations)
                && !this.defineDouble(builder, value, def, validations)
                && !this.defineCollection(builder, value, def, validations)
                && !this.defineMap(builder, value, def, validations))) {
            this.defineNullUnknown(builder, value, def, validations);
        }
        this.warnIfInvalid(value, validations);
    }

    private void defineEnum(Builder builder, ConfigValue value, Object def, Validations validations) {
        builder.defineEnum(
            value.name(), () -> (Enum) def, EnumGetMethod.NAME_IGNORECASE, validations.validator(), (Class<Enum>) value.type());
    }

    private void defineArray(Builder builder, ConfigValue value, Object def, Validations validations) {
        validations.set(Typed.class, new Typed<>(value.type().getComponentType()));
        builder.defineListAllowEmpty(value.name(), ConfigUtil.arrayToList(def), validations.validator());
    }

    private void defineNullUnknown(Builder builder, ConfigValue value, Object def, Validations validations) {
        // this is fine because the validator will handle types for us
        builder.define(value.name(), () -> def, validations.validator());
    }

    private boolean defineInt(Builder builder, ConfigValue value, Object def, Validations validations) {
        if (!ConfigUtil.isInteger(value.type())) return false;
        Range range = validations.take(Range.class, () -> Validation.range(value.type()));
        final int min = Math.clamp(range.min(), Integer.MIN_VALUE, Integer.MAX_VALUE);
        final int max = Math.clamp(range.max(), Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.defineInRange(value.name(), ((Number) def).intValue(), min, max);
        return true;
    }

    private boolean defineLong(Builder builder, ConfigValue value, Object def, Validations validations) {
        if (!ConfigUtil.isLong(value.type())) return false;
        Range range = validations.take(Range.class, () -> Validation.LONG_RANGE);
        builder.defineInRange(value.name(), ((Number) def).longValue(), range.min(), range.max());
        return true;
    }

    private boolean defineDouble(Builder builder, ConfigValue value, Object def, Validations validations) {
        if (!ConfigUtil.isDouble(value.type()) && !ConfigUtil.isFloat(value.type())) return false;
        DecimalRange range = validations.take(DecimalRange.class, () -> Validation.decimalRange(value.type()));
        builder.defineInRange(value.name(), ((Number) def).doubleValue(), range.min(), range.max());
        return true;
    }

    private boolean defineBoolean(Builder builder, ConfigValue value, Object def) {
        if (!ConfigUtil.isBoolean(value.type())) return false;
        builder.define(value.name(), ((Boolean) def).booleanValue());
        return true;
    }

    private boolean defineCollection(Builder builder, ConfigValue value, Object def, Validations validations) {
        if (!Collection.class.isAssignableFrom(value.type())) return false;
        final Class<?> generic = validations.generics()[0];
        validations.set(Typed.class, new Typed<>(ConfigUtil.widen(generic)));
        if (validations.generics().length > 1) {
            final Class<?>[] remainingGenerics = ConfigUtil.shiftGenerics(validations.generics());
            validations.set(GenericTyped.class, new GenericTyped(remainingGenerics));
        } else {
            validations.take(GenericTyped.class);
        }
        builder.defineListAllowEmpty(value.name(), ConfigUtil.collectionToList(def), validations.validator());
        return true;
    }

    private boolean defineMap(
            Builder builder, ConfigValue value, Object def, Validations validations) {
        if (!Map.class.isAssignableFrom(value.type())) return false;
        final Map m = (Map) def;
        if (m.isEmpty()) {
            log.warn("Cannot infer key type (assuming string): {}", value);
        } else {
            final Object k = m.keySet().iterator().next();
            if (!(k instanceof String)) {
                this.error(value, "Map values must have string keys");
                return true;
            }
        } // todo: validate elements instead
        builder.define(value.name(), () -> def, validations.validator());
        return true;
    }

    private void warnIfInvalid(ConfigValue value, Validations validations) {
        for (final Validation<?> v : validations.map().values()) {
            if (!v.isValidForType(value.type())) {
                this.warn(value, "Not valid for type: " + v + " on " + value.name());
            }
        }
    }

    private void updateAll(ForgeConfigSpec spec, List<String> prefix, Object instance, CategoryValue config) {
        for (final ConfigValue value : config.values()) {
            final List<String> path = append(prefix, value.name());
            if (value instanceof CategoryValue category) {
                updateAll(spec, path, value.get(this.mod, instance), category);
                continue;
            }
            final Validations validations = this.validations.get(value);
            value.set(this.mod, instance, ConfigUtilImpl.remap(value.type(), validations.generics(), spec.getValues().getRaw(path)));
        }
    }

    private String filename() {
        return this.config.parent().name();
    }

    private void error(ConfigValue value, String message) {
        this.error(new ValueException(message, this.filename(), value));
    }

    private void error(FormattedException e) {
        LibErrorContext.error(this.mod, e);
    }

    private void warn(ConfigValue value, String message) {
        this.warn(new ValueException(message, this.filename(), value));
    }

    private void warn(FormattedException e) {
        LibErrorContext.warn(this.mod, e);
    }

    private static List<String> append(List<String> prefix, String key) {
        final List<String> l = new ArrayList<>(prefix);
        l.add(key);
        return l;
    }
}
