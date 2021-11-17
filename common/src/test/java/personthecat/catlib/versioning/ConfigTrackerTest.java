package personthecat.catlib.versioning;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.lifecycle.GameReadyEvent;
import personthecat.catlib.util.McUtils;

import java.io.File;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ConfigTrackerTest {

    // Need test implementations since we're running agnostic code.
    private static final MockedStatic<McUtils> MC_UTILS_IMPL = Mockito.mockStatic(McUtils.class);

    private final ModDescriptor testDescriptor =
        ModDescriptor.builder().modId("test").name("Test Mod").build();

    @BeforeAll
    static void before() {
        MC_UTILS_IMPL.when(McUtils::getConfigDir).thenReturn(new File("testOutput/config"));
        MC_UTILS_IMPL.when(McUtils::isClientSide).thenReturn(true);
    }

    @AfterAll
    static void teardown() {
        MC_UTILS_IMPL.close();
    }

    @Test
    public void updatedConfig_isFlagged() {
        ConfigTracker.forMod(this.testDescriptor).track(new TestCache(true)).save();

        final ConfigTracker<TestCache> newCache = ConfigTracker.forMod(this.testDescriptor).track(new TestCache(false));
        assertTrue(newCache.isUpdated());
    }

    @Test
    public void equivalentConfig_isNotFlagged() {
        ConfigTracker.forMod(this.testDescriptor).track(new TestCache(true)).save();

        final ConfigTracker<TestCache> newCache = ConfigTracker.forMod(this.testDescriptor).track(new TestCache(true));
        assertFalse(newCache.isUpdated());
    }

    @Test
    public void missingFile_isFlagged() {
        final File file = ConfigTracker.forMod(this.testDescriptor).track(new TestCache(true)).getFile();
        assertTrue(!file.exists() || file.delete());

        final ConfigTracker<TestCache> newCache = ConfigTracker.forMod(this.testDescriptor).track(new TestCache(true));
        assertTrue(newCache.isUpdated());
    }

    @Test
    public void scheduledEvent_savesAutomatically() {
        ConfigTracker.forMod(this.testDescriptor).track(new TestCache(true)).save();

        final ConfigTracker<TestCache> newCache = ConfigTracker.forMod(this.testDescriptor)
            .scheduleSave(ConfigTracker.PersistOption.MAIN_MENU).track(new TestCache(false));

        GameReadyEvent.CLIENT.invoker().run();
        assertTrue(newCache.isSaved());
        assertTrue(GameReadyEvent.CLIENT.isEmpty());
    }

    private static class TestCache implements Serializable {
        final boolean value;

        TestCache(final boolean value) {
            this.value = value;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof TestCache) {
                return this.value == ((TestCache) o).value;
            }
            return false;
        }
    }
}
