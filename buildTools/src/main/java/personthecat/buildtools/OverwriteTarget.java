package personthecat.buildtools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given class should be overwritten in all child source sets.
 * <p>
 *   This marker is <b>not currently validated</b> by {@link OverwriteValidator}.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OverwriteTarget {}
