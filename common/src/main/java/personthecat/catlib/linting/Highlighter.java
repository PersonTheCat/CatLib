package personthecat.catlib.linting;

import net.minecraft.network.chat.Component;

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
     * This interface represents an object which tracks and applies
     * formatting changes to a body of text. It houses the logic
     * responsible for locating the text and will provide the updated
     * text when it becomes available.
     */
    interface Instance {
        void next();
        boolean found();
        int start();
        int end();
        Component replacement();
    }
}
