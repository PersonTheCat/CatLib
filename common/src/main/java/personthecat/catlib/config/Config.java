package personthecat.catlib.config;

import org.intellij.lang.annotations.RegExp;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Documented
@SuppressWarnings({"FieldMayBeFinal", "unused"})
public @interface Config {

    interface Listener {
        default void onInstanceCreated(CategoryValue config) {}
        default void onConfigUpdated() throws ValidationException {}
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Comment {
        String value();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface GenericType {
        Class<?>[] value();
        String message() default Validation.INVALID_TYPE;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Range {
        long min() default Long.MIN_VALUE;
        long max() default Long.MAX_VALUE;
        String message() default Validation.OUT_OF_BOUNDS;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface DecimalRange {
        double min() default Double.MIN_VALUE;
        double max() default Double.MAX_VALUE;
        String message() default Validation.OUT_OF_BOUNDS;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Regex {
        @RegExp String value();
        String message() default Validation.PATTERN_MISMATCH;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NotBlank {
        String message() default Validation.BLANK_TEXT;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NotNull {
        String message() default Validation.NULL_VALUE;
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NeedsWorldRestart {}

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface CanBeNull {}
}
