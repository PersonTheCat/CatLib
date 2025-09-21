package personthecat.catlib.config;

import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.FormattedException;

import java.util.Collection;

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

    public ModDescriptor getMod() {
        return this.mod;
    }

    public CategoryValue getConfig() {
        return this.config;
    }

    public Object getInstance() {
        return this.instance;
    }

    protected void setValue(ConfigValue value, Object instance, Object o) throws ValidationException {
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
        this.validate(value, validations, o);
        value.set(this.mod, instance, o);
    }

    public Validations getValidations(ConfigValue value) {
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

    protected void validate(ConfigValue value, Validations validations, Object o) throws ValidationException {
        if (!ConfigUtil.isSupportedGenericType(value.type())) {
            Validation.validate(validations.values(), this.filename(), value, o);
            return;
        }
        Validation.validate(validations.typeValidations(), this.filename(), value, o);
        final Collection<Validation<?>> entryValidations = validations.entryValidations().values();
        for (final Object e : ConfigUtil.getElements(o)) {
            Validation.validate(entryValidations, this.filename(), value, e);
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

    public String filename() {
        return this.config.parent().name();
    }

    protected void error(ConfigValue value, String message) {
        this.error(new ValueException(message, this.filename(), value));
    }

    protected void error(ConfigValue value, String message, Throwable cause) {
        this.error(new ValueException(message, this.filename(), value, cause));
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
