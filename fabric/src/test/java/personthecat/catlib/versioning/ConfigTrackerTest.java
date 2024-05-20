package personthecat.catlib.versioning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.lifecycle.GameReadyEvent;
import personthecat.catlib.test.McBootstrapExtension;

import java.io.File;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledOnOs(OS.WINDOWS)
@ExtendWith(McBootstrapExtension.class)
public final class ConfigTrackerTest {

    private ModDescriptor testDescriptor;

    @BeforeEach
    public void setup() {
        this.testDescriptor = ModDescriptor.builder().modId("test").name("Test Mod").build();
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
            .scheduleSave(ConfigTracker.PersistOption.GAME_READY).track(new TestCache(false));

        GameReadyEvent.COMMON.invoker().run();
        assertTrue(newCache.isSaved());
        assertTrue(GameReadyEvent.COMMON.isEmpty());
    }

    private record TestCache(boolean value) implements Serializable {}
}
