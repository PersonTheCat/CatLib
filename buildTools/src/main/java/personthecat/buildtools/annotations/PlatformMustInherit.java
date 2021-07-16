package personthecat.buildtools.annotations;

import personthecat.buildtools.OverwriteValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given method <b>must be overwritten</b> by all child
 * source sets. This solution is compile safe and will be validated by
 * {@link OverwriteValidator}.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
public @interface PlatformMustInherit {
}
