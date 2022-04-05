package personthecat.catlib.data;

import org.junit.jupiter.api.Test;
import personthecat.catlib.data.collections.NonRecursiveObserverSet;
import personthecat.catlib.data.collections.ObserverSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class NonRecursiveObserverSetTest {

    @Test
    public void nestedIteration_skipsCurrentListener() {
        final List<Integer> output = new ArrayList<>();
        final ObserverSet<Runnable> set = new NonRecursiveObserverSet<>();
        set.add(() -> output.add(1));
        set.add(() -> { output.add(2); set.forEach(Runnable::run); });
        set.add(() -> output.add(3));

        set.forEach(Runnable::run);

        assertEquals(Arrays.asList(1, 2, 1, 3, 3), output);
    }

    @Test
    public void erredEntries_doNotStayActive() {
        final ObserverSet<Runnable> set = new NonRecursiveObserverSet<>();
        set.add(() -> { throw new RuntimeException(); });

        try {
            set.forEach(Runnable::run);
        } catch (final RuntimeException ignored) {}

        assertFalse(set.hasActiveEntries());
    }
}
