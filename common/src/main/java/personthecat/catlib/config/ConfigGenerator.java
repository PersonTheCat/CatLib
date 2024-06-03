package personthecat.catlib.config;

import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.FormattedException;

public abstract class ConfigGenerator {
    protected final ModDescriptor mod;
    protected final CategoryValue config;
    protected final Object instance;
    private final ValidationMap validations;

    public ConfigGenerator(ModDescriptor mod, CategoryValue config) {
        this.mod = mod;
        this.config = config;
        this.instance = config.parent().get(mod, null);
        this.validations = new ValidationMap();
    }

    protected void setValue(ConfigValue value, Object instance, Object o) {
        final Validations validations = this.getValidations(value);
        if (validations == null) {
            return;
        }
        try {
            o = ConfigUtil.remap(value.type(), validations.generics(), o);
        } catch (final RuntimeException e) {
            this.warn(value, e.getMessage());
            return;
        }
        try {
            Validation.validate(validations.values(), this.filename(), value, o);
        } catch (final ValidationException e) {
            this.warn(e);
            return;
        }
        value.set(this.mod, instance, o);
    }

    protected Validations getValidations(ConfigValue value) {
        if (this.validations.containsKey(value)) {
            return this.validations.get(value);
        }
        try {
            final Validations validations = Validations.fromValue(this.filename(), value);
            this.warnIfInvalid(value, validations);
            this.validations.put(value, validations);
            return validations;
        } catch (final ValueException e) {
            this.error(e);
            this.validations.put(value, null);
            return null;
        }
    }

    private void warnIfInvalid(ConfigValue value, Validations validations) {
        final Class<?> generic = validations.genericType();
        for (final Validation<?> v : validations.map().values()) {
            if (!v.isValidForType(value.type()) && !v.isValidForType(generic)) {
                this.warn(value, "Not valid for type: " + v + " on " + value.name());
            }
        }
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

    protected String filename() {
        return this.config.parent().name();
    }

    protected void error(ConfigValue value, String message) {
        this.error(new ValueException(message, this.filename(), value));
    }

    protected void error(FormattedException e) {
        LibErrorContext.error(this.mod, e);
    }

    protected void warn(ConfigValue value, String message) {
        this.warn(new ValueException(message, this.filename(), value));
    }

    protected void warn(FormattedException e) {
        LibErrorContext.warn(this.mod, e);
    }
}
