package personthecat.catlib.linting;

/**
 * This interface represents any object storing instructions for how to
 * highlight a body of text. It does not contain the text, nor should it
 * contain any mutable data for tracking the text. Rather, it should
 * provide an {@link Instance} which does once the text becomes available.
 */
public interface Highlighter {

    /**
     * Get an instance with data for tracking matches in the given text.
     *
     * @param text The text being highlighted
     * @return A highlighter instance matching this specific text
     */
    Instance get(String text);

    /**
     * Indicates whether this highlighter is intended to replace text
     * in its entirety. If the highlighter is non-atomic, the {@link
     * LinterDelegate} engine may poll the instance for nested slices
     * within the matched boundaries.
     *
     * @return <code>true</code> if the highlighter is atomic.
     */
    default boolean atomic() {
        return true;
    }

    /**
     * If applicable, coerces this highlighter to support non-atomic
     * sub-slice matching and replacement.
     *
     * @return A configuration of this Highlighter supporting non-atomic
     *         replacement semantics.
     * @throws UnsupportedOperationException if this instance does not
     *         support non-atomic replacement semantics.
     */
    default NonAtomic canBeSplit() {
        return this::get;
    }

    /**
     * This interface represents an object which tracks and applies
     * formatting changes to a body of text. It houses the logic
     * responsible for locating the text and will provide the updated
     * text when it becomes available.
     */
    interface Instance {
        /**
         * Jump to the next match.
         *
         * <p>The implementation is assumed to have the first match available upon construction.
         */
        void next();

        /**
         * Indicates if a matched slice was found.
         *
         * @return <code>true</code> if a match was found.
         */
        boolean found();

        /**
         * The beginning of the matched text.
         *
         * @return The <em>inclusive</em> first index of the matched text.
         */
        int start();

        /**
         * The end of the matched text.
         *
         * @return The <em>exclusive</em> last index of the matched text.
         */
        int end();

        /**
         * Produce a {@link Linter} which describes how to format the current slice of text
         * (or a sub-slice of the matched region).
         *
         * <p>The implementation does not need to check atomicity; the engine should consult
         * {@link Highlighter#atomic()} to decide whether invoke this linter on sub-ranges.
         *
         * @return A function describing how to format a slice of text.
         */
        Linter match();
    }

    /**
     * Variant of {@link Highlighter} which explicitly disallows coercion
     * to use non-atomic semantics.
     */
    interface Atomic extends Highlighter {

        @Override
        default NonAtomic canBeSplit() {
            throw new UnsupportedOperationException("Non-atomic semantics not supported for " + this.getClass().getSimpleName());
        }
    }

    /**
     * Variant of {@link Highlighter} with default non-atomic behavior.
     */
    interface NonAtomic extends Highlighter {
        @Override
        default boolean atomic() {
            return false;
        }

        @Override
        default NonAtomic canBeSplit() {
            return this;
        }
    }
}
