package personthecat.catlib.versioning;

import lombok.extern.log4j.Log4j2;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.lifecycle.ClientReadyEvent;
import personthecat.catlib.event.world.CommonWorldEvent;
import personthecat.catlib.io.FileIO;
import personthecat.catlib.util.LibReference;
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
    @Nullable private final Runnable clientReady;
    @Nullable private final Consumer<LevelAccessor> worldLoad;

    private ConfigTracker(final Builder builder, final T current) {
        this.mod = builder.mod;
        this.file = createFile(builder);
        this.current = current;
        this.cached = readCached(this.file);
        this.updated = !this.current.equals(cached);
        this.saved = false;
        this.clientReady = createClientReady(this, builder.persist);
        this.worldLoad = createWorldLoad(this, builder.persist);
    }

    public static Builder forMod(final ModDescriptor mod) {
        return new Builder(mod);
    }

    private static File createFile(final Builder builder) {
        return new File(McUtils.getConfigDir(), LibReference.MOD_ID + "/versioning/"
            + builder.mod.getModId() + "/" + builder.category + ".cft");
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T readCached(final File file) {
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
    private static Runnable createClientReady(final ConfigTracker<?> tracker, final PersistOption persist) {
        if (persist == PersistOption.MAIN_MENU) {
            final Runnable clientReady = tracker::save;
            ClientReadyEvent.EVENT.register(clientReady);
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
            log.error("Error saving config tracker for " + this.mod.getName()
                + ". This will eventually be logged in the error menu.", e);
        }
    }

    public void deregister() {
        if (this.clientReady != null) {
            ClientReadyEvent.EVENT.deregister(this.clientReady);
        } else if (this.worldLoad != null) {
            CommonWorldEvent.LOAD.deregister(this.worldLoad);
        }
    }

    @Nullable
    public T getCached() {
        return this.cached;
    }

    public T getCachedOrDefault() {
        return this.cached != null ? this.cached : this.current;
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

        @SuppressWarnings("ConstantConditions")
        public Builder scheduleSave(final PersistOption persist) {
            if (persist == PersistOption.MAIN_MENU && McUtils.isDedicatedServer()) {
                this.persist = PersistOption.WORLD_LOAD;
            } else {
                this.persist = persist;
            }
            return this;
        }

        public <T extends Serializable> ConfigTracker<T> track(final T current) {
            return new ConfigTracker<>(this, current);
        }
    }

    public enum PersistOption {
        MAIN_MENU,
        WORLD_LOAD,
        MANUAL;
    }
}
