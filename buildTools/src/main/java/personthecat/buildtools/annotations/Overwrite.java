package personthecat.buildtools.annotations;

import personthecat.buildtools.OverwriteValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field or method is designed to overwrite a class member
 * in a child source set. This solution is compile safe and will be validated
 * by {@link OverwriteValidator}.
 * <p>
 *   This annotation can viewed as a counterpart to {@link Override}, as it
 *   bears a similar functionality.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface Overwrite {}
