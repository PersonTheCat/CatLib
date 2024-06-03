package personthecat.catlib.config;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface Validation<T> extends Predicate<T> {
    Range BYTE_RANGE = new Range(Byte.MIN_VALUE, Byte.MAX_VALUE);
    Range SHORT_RANGE = new Range(Short.MIN_VALUE, Short.MAX_VALUE);
    Range INT_RANGE = new Range(Integer.MIN_VALUE, Integer.MAX_VALUE);
    Range LONG_RANGE = new Range(Long.MIN_VALUE, Long.MAX_VALUE);
    DecimalRange FLOAT_RANGE = new DecimalRange(-Float.MAX_VALUE, Float.MAX_VALUE);
    DecimalRange DOUBLE_RANGE = new DecimalRange(-Double.MAX_VALUE, Double.MAX_VALUE);
    String INVALID_TYPE = "catlib.errorText.invalidType";
    String OUT_OF_BOUNDS = "catlib.errorText.outOfBounds";
    String PATTERN_MISMATCH = "catlib.errorText.patternMismatch";
    String BLANK_TEXT = "catlib.errorText.blank";
    String NULL_VALUE = "catlib.errorText.null";

    Class<T> type();
    String message();

    default Component getErrorText(ConfigValue value, Object o) {
        return this.getDetailText(value, value.name());
    }

    default Component getDetailText(ConfigValue value, Object o) {
        return Component.translatable(this.message(), o);
    }

    default @Nullable String getComment() {
        return null;
    }

    default boolean requiresExactType() {
        return false;
    }

    default boolean isValidForType(final Class<?> type) {
        return Collection.class.isAssignableFrom(type)
            || Map.class.isAssignableFrom(type)
            || this.type().isAssignableFrom(type);
    }

    static Validation<Object> from(final Config.GenericType genericType) {
        return new GenericTyped(genericType.value(), genericType.message());
    }

    static Validation<Number> from(final Config.Range range) {
        return new Range(range.min(), range.max(), range.message());
    }

    static Validation<Number> from(final Config.DecimalRange range) {
        return new DecimalRange(range.min(), range.max(), range.message());
    }

    static Validation<String> from(final Config.Regex regex) {
        return new Regex(Pattern.compile(regex.value()), regex.message());
    }

    static Validation<String> from(final Config.NotBlank notBlank) {
        return new NotBlank(notBlank.message());
    }

    static Validation<Object> from(final Config.NotNull notNull) {
        return new NotNull(notNull.message());
    }

    static Range range(Class<?> c) {
        if (Byte.class.isAssignableFrom(c) || byte.class.isAssignableFrom(c)) return BYTE_RANGE;
        if (Short.class.isAssignableFrom(c) || short.class.isAssignableFrom(c)) return SHORT_RANGE;
        if (Integer.class.isAssignableFrom(c) || int.class.isAssignableFrom(c)) return INT_RANGE;
        return LONG_RANGE;
    }

    static DecimalRange decimalRange(Class<?> c) {
        if (Float.class.isAssignableFrom(c) || float.class.isAssignableFrom(c)) return FLOAT_RANGE;
        return DOUBLE_RANGE;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean isValid(Iterable<Validation<?>> list, Object value) {
        for (final Validation<?> v : list) {
            if (value == null || v.isValidForType(value.getClass())) {
                if (!((Validation) v).test(value)) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void validate(
            Iterable<Validation<?>> list, String filename, ConfigValue c, Object value) throws ValidationException {
        for (final Validation<?> v : list) {
            if (value == null || v.isValidForType(value.getClass())) {
                if (!((Validation) v).test(value)) {
                    final Object o = value != null ? value : "null";
                    final Component error = v.getErrorText(c, o);
                    final Component details = v.getDetailText(c, o);
                    throw new ValidationException(filename, c.name(), error, details);
                }
            }
        }
    }

    static String buildComment(Iterable<Validation<?>> list, Class<?>... skip) {
        final List<Class<?>> skipList = Arrays.asList(skip);
        final StringBuilder sb = new StringBuilder();
        for (final Validation<?> v : list) {
            if (skipList.contains(v.getClass())) {
                continue;
            }
            final String comment = v.getComment();
            if (comment == null) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(comment);
        }
        return sb.toString();
    }

    record Typed<T>(Class<T> type) implements Validation<T> {

        @Override
        public boolean test(final T t) {
            return t == null || this.isValidForType(t.getClass());
        }

        @Override
        public String message() {
            return INVALID_TYPE;
        }

        @Override
        public Component getErrorText(ConfigValue value, Object o) {
            return Component.translatable(this.message(), value.name(), value.type().getSimpleName());
        }

        @Override
        public Component getDetailText(ConfigValue value, Object o) {
            return Component.translatable(this.message(), o.getClass().getSimpleName(), value.type().getSimpleName());
        }
    }

    record GenericTyped(Class<?>[] types, String message) implements Validation<Object> {

        public GenericTyped(Class<?>[] types) {
            this(types, INVALID_TYPE);
        }

        @Override
        public boolean test(final Object o) {
            return testRecursive(this.types, 0, o);
        }

        private static boolean testRecursive(Class<?>[] types, int idx, Object o) {
            if (idx >= types.length) {
                return true;
            }
            final Class<?> t = types[idx];
            if (o.getClass().isArray()) {
                o = ConfigUtil.arrayToList(o);
            } else if (o instanceof Map<?, ?> m) {
                o = m.values();
            }
            if (o instanceof Collection<?> c) {
                for (final Object e : c) {
                    if (!t.isInstance(e)) {
                        return false;
                    }
                    if (!testRecursive(types, idx + 1, e)) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public Class<Object> type() {
            return Object.class;
        }

        @Override
        public Component getErrorText(ConfigValue value, Object o) {
            return Component.translatable(this.message(), value.name(), value.type().getSimpleName());
        }

        @Override
        public Component getDetailText(ConfigValue value, Object o) {
            return Component.translatable(this.message(), o.getClass().getSimpleName(), value.type().getSimpleName());
        }
    }

    record NotNull(String message) implements Validation<Object> {

        public NotNull() {
            this(NULL_VALUE);
        }

        @Override
        public boolean test(Object o) {
            return o != null;
        }

        @Override
        public Class<Object> type() {
            return Object.class;
        }
    }

    record Range(long min, long max, String message) implements Validation<Number> {

        public Range(long min, long max) {
            this(min, max, OUT_OF_BOUNDS);
        }

        @Override
        public boolean test(final Number l) {
            return l != null && l.longValue() >= this.min && l.longValue() <= this.max;
        }

        @Override
        public Class<Number> type() {
            return Number.class;
        }

        @Override
        public String getComment() {
            if (this.min == Long.MIN_VALUE) {
                if (this.max != Long.MAX_VALUE) {
                    return "Range: < " + this.max;
                }
            } else if (this.max == Long.MAX_VALUE) {
                return "Range: > " + this.min;
            }
            return "Range: " + this.min + " ~ " + this.max;
        }
    }

    record DecimalRange(double min, double max, String message) implements Validation<Number> {

        public DecimalRange(double min, double max) {
            this(min, max, OUT_OF_BOUNDS);
        }

        @Override
        public boolean test(final Number d) {
            return d != null && d.doubleValue() >= this.min && d.doubleValue() <= this.max;
        }

        @Override
        public Class<Number> type() {
            return Number.class;
        }

        @Override
        public String getComment() {
            if (this.min == -Double.MAX_VALUE) {
                if (this.max != Double.MAX_VALUE) {
                    return "Range: < " + this.max;
                }
            } else if (this.max == Double.MAX_VALUE) {
                return "Range: > " + this.min;
            }
            return "Range: " + this.min + " ~ " + this.max;
        }
    }

    record Regex(Pattern pattern, String message) implements Validation<String> {

        public Regex(Pattern pattern) {
            this(pattern, PATTERN_MISMATCH);
        }

        @Override
        public boolean test(final String s) {
            return s != null && this.pattern.matcher(s).matches();
        }

        @Override
        public Class<String> type() {
            return String.class;
        }

        @Override
        public String getComment() {
            return "Pattern: " + this.pattern;
        }
    }

    record NotBlank(String message) implements Validation<String> {

        public NotBlank() {
            this(BLANK_TEXT);
        }

        @Override
        public boolean test(final String s) {
            return s != null && !s.isBlank();
        }

        @Override
        public Class<String> type() {
            return String.class;
        }
    }
}
