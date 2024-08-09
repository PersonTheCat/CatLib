package personthecat.catlib.command.annotations;

import personthecat.catlib.command.CommandClassEvaluator;

/**
 * Used to indicate (at runtime) that a command argument is optional.
 *
 * <p>Note that any annotation of the same name can be used, so long
 * as it is also available to the {@link CommandClassEvaluator evaluator}
 * at runtime.
 */
public @interface Nullable {}
