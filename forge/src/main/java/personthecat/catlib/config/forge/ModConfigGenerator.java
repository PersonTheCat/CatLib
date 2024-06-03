package personthecat.catlib.config.forge;

import com.electronwill.nightconfig.core.EnumGetMethod;
import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import personthecat.catlib.config.CategoryValue;
import personthecat.catlib.config.ConfigGenerator;
import personthecat.catlib.config.ConfigUtil;
import personthecat.catlib.config.ConfigValue;
import personthecat.catlib.config.Validation;
import personthecat.catlib.config.Validation.DecimalRange;
import personthecat.catlib.config.Validation.Range;
import personthecat.catlib.config.ValidationException;
import personthecat.catlib.config.Validations;
import personthecat.catlib.data.ModDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ModConfigGenerator extends ConfigGenerator {

    public ModConfigGenerator(ModDescriptor mod, CategoryValue config) {
        super(mod, config);
    }

    public ForgeConfigSpec generateSpec() {
        final Builder builder = new Builder();
        for (final ConfigValue v : config.values()) {
            this.apply(builder, v);
        }
        return builder.build();
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
        final Validations validations = this.getValidations(value);
        if (validations == null) {
            return;
        }
        // types that have automatic comment details
        if (this.defineEnum(builder, value, def, validations)) {
            return;
        } else if (def != null) {
            if (this.defineBoolean(builder, value, def)
                    || this.defineInt(builder, value, def, validations)
                    || this.defineLong(builder, value, def, validations)
                    || this.defineDouble(builder, value, def, validations)) {
                return;
            }
        }
        final String details = Validation.buildComment(validations.values());
        if (!details.isEmpty()) {
            builder.comment(details);
        }
        if (this.defineArray(builder, value, def, validations)
                || this.defineCollection(builder, value, def, validations)) {
            return;
        }
        this.defineNullUnknown(builder, value, def, validations);
    }

    private boolean defineEnum(Builder builder, ConfigValue value, Object def, Validations validations) {
        if (!value.type().isEnum()) return false;
        builder.defineEnum(
            value.name(), () -> (Enum) def, EnumGetMethod.NAME_IGNORECASE, validations.typeValidator(), (Class<Enum>) value.type());
        return true;
    }

    private boolean defineArray(Builder builder, ConfigValue value, Object def, Validations validations) {
        if (!value.type().isArray()) return false;
        builder.defineListAllowEmpty(value.name(), ConfigUtil.arrayToList(def), validations.entryTypeValidator());
        return true;
    }

    private void defineNullUnknown(Builder builder, ConfigValue value, Object def, Validations validations) {
        // this is fine because the validator will handle types for us
        builder.define(Lists.newArrayList(value.name()), () -> def, validations.typeValidator(), value.type());
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
        builder.defineListAllowEmpty(value.name(), ConfigUtil.collectionToList(def), validations.entryTypeValidator());
        return true;
    }

    private void updateAll(ForgeConfigSpec spec, List<String> prefix, Object instance, CategoryValue config) {
        for (final ConfigValue value : config.values()) {
            final List<String> path = append(prefix, value.name());
            if (value instanceof CategoryValue category) {
                updateAll(spec, path, value.get(this.mod, instance), category);
                continue;
            }
            final ForgeConfigSpec.ConfigValue forgeValue = spec.getValues().getRaw(path);
            try {
                this.setValue(value, instance, forgeValue.get());
            } catch (final ValidationException e) {
                this.warn(e);
                forgeValue.set(forgeValue.getDefault());
                value.set(this.mod, instance, value.defaultValue());
            }
        }
    }

    private static List<String> append(List<String> prefix, String key) {
        final List<String> l = new ArrayList<>(prefix);
        l.add(key);
        return l;
    }
}
