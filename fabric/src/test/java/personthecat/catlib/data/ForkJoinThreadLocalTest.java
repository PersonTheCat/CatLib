package personthecat.catlib.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ForkJoinThreadLocalTest {
    // this number should be lower than the actual thread count due to the test env
    private static final int PARALLELISM = 4;

    private static final ForkJoinThreadLocal<Data> FORK_LOCAL = ForkJoinThreadLocal.eager(Data::new, false);
    private static final ForkJoinPool POOL = new ForkJoinPool(PARALLELISM);

    @Test
    public void get_returnsUniqueValues_perThread() {
        final var futures = new ArrayList<ForkJoinTask<Data>>();
        for (int i = 0; i < PARALLELISM; i++) {
            futures.add(POOL.submit(() -> {
                Thread.sleep(4); // we just need a small pause to ensure multithreading
                return FORK_LOCAL.get();
            }));
        }
        final var values = futures.stream().map(ForkJoinTask::join).collect(Collectors.toSet());
        assertEquals(PARALLELISM, values.size());
    }

    @Test
    public void get_returnsSameValue_withinThread() {
        assertTrue(POOL.submit(() -> {
            final var previous = FORK_LOCAL.get();
            for (int i = 0; i < 1_000; i++) {
                if (previous != FORK_LOCAL.get()) {
                    return false;
                }
            }
            return true;
        }).join());
    }

    @Test
    public void set_updatesValues_withinThread() {
        assertTrue(POOL.submit(() -> {
            final var original = FORK_LOCAL.get();
            FORK_LOCAL.set(new Data());
            return FORK_LOCAL.get() != original;
        }).join());
    }

    @Test
    public void runScoped_hasValue_untilEndOfScope() {
        assertTrue(POOL.submit(() -> {
            final var original = FORK_LOCAL.get();
            final var updated = new Data();
            final var check = new AtomicBoolean();
            FORK_LOCAL.runScoped(updated, () ->
                check.set(FORK_LOCAL.get() == updated));
            return check.get() && FORK_LOCAL.get() == original;
        }).join());
    }

    @Test
    public void getScoped_hasValue_untilEndOfScope() {
        assertTrue(POOL.submit(() -> {
            final var original = FORK_LOCAL.get();
            final var updated = new Data();
            final var check = FORK_LOCAL.getScoped(updated, () ->
                FORK_LOCAL.get() == updated);
            return check && FORK_LOCAL.get() == original;
        }).join());
    }

    @Test
    public void remove_deletesValue() {
        assertTrue(POOL.submit(() -> {
            FORK_LOCAL.remove();
            if (FORK_LOCAL.get() == null) {
                FORK_LOCAL.set(new Data());
                return true;
            }
            return false;
        }).join());
    }

    @Test
    public void get_handlesFallbackScenario() throws InterruptedException {
        final var ref = new AtomicReference<Data>();
        final var thread = new Thread(() -> ref.set(FORK_LOCAL.get()));
        thread.start();
        thread.join();

        assertNotNull(ref.get());
        assertEquals(-1, ref.get().threadId);
    }

    private static class Data {
        int threadId = ForkJoinThreadLocal.getThreadIndex();
    }
}