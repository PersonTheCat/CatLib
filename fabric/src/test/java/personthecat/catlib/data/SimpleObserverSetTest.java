package personthecat.catlib.data;

import org.junit.jupiter.api.Test;
import personthecat.catlib.data.collections.ObserverSet;
import personthecat.catlib.data.collections.SimpleObserverSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SimpleObserverSetTest {

    @Test
    public void forEach_supportConcurrentRemoval() {
        final List<Integer> output = new ArrayList<>();
        final ObserverSet<Runnable> set = new SimpleObserverSet<>();

        final Runnable add1 = () -> output.add(1);
        final Runnable add0 = () -> {
            output.add(0);
            set.remove(add1);
        };

        set.add(add0);
        set.add(add1);

        set.forEach(Runnable::run);

        assertEquals(1, output.size());
        assertEquals(0, (int) output.get(0));
    }

    @Test
    public void forEach_supportConcurrentClear() {
        final List<Integer> output = new ArrayList<>();
        final ObserverSet<Runnable> set = new SimpleObserverSet<>();
        set.add(() -> {  output.add(0); set.clear(); });
        set.add(() -> output.add(1));

        set.forEach(Runnable::run);

        assertEquals(1, output.size());
        assertEquals(0, (int) output.get(0));
    }

    @Test
    public void listener_canRemoveItself() {
        final ObserverSet<Runnable> set = new SimpleObserverSet<>();
        set.add(new Runnable() {
            @Override public void run() {
                set.remove(this);
            }
        });
        assertDoesNotThrow(() -> set.forEach(Runnable::run));
        assertTrue(set.isEmpty());
    }

    @Test
    public void addListener_isAvailableOnSubsequentRun() {
        final List<Integer> output = new ArrayList<>();
        final ObserverSet<Runnable> set = new SimpleObserverSet<>();
        set.add(() -> { output.add(0); set.add(() -> output.add(1)); });

        set.forEach(Runnable::run);
        set.forEach(Runnable::run);

        assertEquals(Arrays.asList(0, 0, 1), output);
    }
}
