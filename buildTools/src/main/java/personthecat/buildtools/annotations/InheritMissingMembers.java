package personthecat.buildtools.annotations;

import personthecat.buildtools.OverwriteValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used at the class level, indicates that any member not present in the
 * annotated class will be defined and copied from the common source set.
 * <p>
 *   When used on a method or field, indicates that the body or value of the member
 *   will be transformed to that of the common source set at compile time.
 * </p>
 * <p>
 *   This solution is compile safe and will validated by {@link OverwriteValidator}.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface InheritMissingMembers {}
