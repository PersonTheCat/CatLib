package personthecat.catlib.versioning;

import lombok.extern.log4j.Log4j2;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.CatLib;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.lifecycle.GameReadyEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.exception.UnreachableException;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.util.McUtils;

import java.io.*;
import java.util.function.Consumer;

@Log4j2
public class ConfigTracker<T extends Serializable> {
    private final ModDescriptor mod;
    private final File file;
    private volatile T current;
    @Nullable private final T cached;
    private final boolean updated;
    private volatile boolean saved;
    @Nullable private final Runnable gameReady;
    @Nullable private final Consumer<LevelAccessor> worldLoad;

    protected ConfigTracker(final Builder builder, final T current) {
        this.mod = builder.mod;
        this.file = createFile(builder);
        this.current = current;
        this.cached = readCached(this.file);
        this.updated = !this.current.equals(cached);
        this.saved = false;
        this.gameReady = gameReady(this, builder.persist);
        this.worldLoad = createWorldLoad(this, builder.persist);
    }

    public static Builder forMod(final ModDescriptor mod) {
        return new Builder(mod);
    }

    private static File createFile(final Builder builder) {
        return new File(McUtils.getConfigDir(), CatLib.ID + "/versioning/"
            + builder.mod.getModId() + "/" + builder.category + ".cft");
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T readCached(final File file) {
        if (!file.exists()) return null;
        try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (T) ois.readObject();
        } catch (final IOException e) {
            log.error("Error reading config tracker. This will eventually be logged in the error menu.");
        } catch (final ClassNotFoundException e) {
            log.warn("Original tracked class was updated. Ignoring...", e);
        }
        return null;
    }

    @Nullable
    private static Runnable gameReady(final ConfigTracker<?> tracker, final PersistOption persist) {
        if (persist == PersistOption.GAME_READY) {
            final Runnable clientReady = tracker::save;
            GameReadyEvent.COMMON.register(clientReady);
            return clientReady;
        }
        return null;
    }

    @Nullable
    private static Consumer<LevelAccessor> createWorldLoad(final ConfigTracker<?> tracker, final PersistOption persist) {
        if (persist == PersistOption.WORLD_LOAD) {
            final Consumer<LevelAccessor> worldLoad = a -> tracker.save();
            CommonWorldEvent.LOAD.register(worldLoad);
            return worldLoad;
        }
        return null;
    }

    public boolean isUpdated() {
        return this.updated;
    }

    public File getFile() {
        return this.file;
    }

    public void save() {
        if (this.updated) {
            this.writeCurrent();
            this.deregister();
        }
    }

    public void writeUpdated(final T updated) {
        this.setCurrent(updated);
        this.writeCurrent();
        this.deregister();
    }

    public T getCurrent() {
        return this.current;
    }

    public synchronized void setCurrent(final T current) {
        this.current = current;
    }

    private synchronized void writeCurrent() {
        FileIO.mkdirsOrThrow(this.file.getParentFile());

        try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(this.file))) {
            oos.writeObject(this.current);
            oos.flush();
            this.saved = true;
        } catch (final IOException e) {
            log.error("Error saving config tracker for {}. This will eventually be logged in the error menu.",
                this.mod.getName(), e);
        }
    }

    public void deregister() {
        if (this.gameReady != null) {
            GameReadyEvent.COMMON.deregister(this.gameReady);
        } else if (this.worldLoad != null) {
            CommonWorldEvent.LOAD.deregister(this.worldLoad);
        }
    }

    @Nullable
    public T getCached() {
        return this.cached;
    }

    public T getCachedOrCurrent() {
        return this.cached != null ? this.cached : this.current;
    }

    public T getCachedOrDefault(final T def) {
        return this.cached != null ? this.cached : def;
    }

    public boolean isSaved() {
        return this.saved;
    }

    public static class Builder {
        private final ModDescriptor mod;
        private String category = "common";
        private PersistOption persist = PersistOption.MANUAL;

        private Builder(final ModDescriptor mod) {
            this.mod = mod;
        }

        public Builder withCategory(final String category) {
            this.category = category;
            return this;
        }

        public Builder scheduleSave(final PersistOption persist) {
            this.persist = persist;
            return this;
        }

        public <T extends Serializable> ConfigTracker<T> track(final T current) {
            checkEqualsImplementation(current);
            return new ConfigTracker<>(this, current);
        }

        private static void checkEqualsImplementation(final Object o) {
            try {
                final Class<?> type = o.getClass();
                if (type.getMethod("equals", Object.class).getDeclaringClass() == Object.class) {
                    throw new AssertionError(type.getSimpleName() + " must provide explicit equals implementation");
                }
            } catch (final NoSuchMethodException ignored) {
                throw new UnreachableException();
            }
        }
    }

    public enum PersistOption {
        GAME_READY,
        WORLD_LOAD,
        MANUAL
    }
}
