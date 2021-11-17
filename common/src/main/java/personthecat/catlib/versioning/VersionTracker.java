package personthecat.catlib.versioning;

import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.lifecycle.GameReadyEvent;

public class VersionTracker extends ConfigTracker<Version> {

    private final int change;

    /**
     * Variant of {@link ConfigTracker#ConfigTracker} designed specifically for comparing
     * semantic versions. This enables the library to expose a semantically correct
     * implementation of {@link ConfigTracker#isUpdated}.
     *
     * @param builder A configuration containing metadata for a given mod.
     * @param current The current version of the mod.
     */
    private VersionTracker(final Builder builder, final Version current) {
        super(builder, current);
        this.change = this.getCached() != null ? this.getCached().compareTo(current) : 0;
    }

    /**
     * Convenience method for tracking mod updates. Call this method with your {@link ModDescriptor}
     * to acquire a handle on the most recent {@link Version} of your mod.
     *
     * <p>Your mod's current version, provided by {@link ModDescriptor#getVersion}, will be persisted
     * on {@link GameReadyEvent#COMMON}.
     *
     * <p>For example, to check whether your mod has been updated by the user, call
     * {@link VersionTracker#isUpgraded}, as follows:
     * <pre>{@code
     *   VersionTracker.trackModVersion(MOD_DESCRIPTOR).isUpgraded();
     * }</pre>
     *
     * <p>Additionally, you can use this handle to read the most recent version of your mod, as follows:
     * <pre>{@code
     *   final VersionTracker versionCache = trackModVersion(MOD_DESCRIPTOR);
     *   final Version previousVersion = versionCache.getCachedOrCurrent();
     * }</pre>
     *
     * <p>It is highly recommended to only call this method a single time as it requires a bit of file
     * IO, which may be expensive.
     *
     * @param mod A descriptor providing details about your mod including its <b>current</b> version.
     * @return A tracker containing data about the last configured version of your mod.
     */
    public static VersionTracker trackModVersion(final ModDescriptor mod) {
        final Builder builder = ConfigTracker.forMod(mod).withCategory("version")
            .scheduleSave(ConfigTracker.PersistOption.GAME_READY);
        return new VersionTracker(builder, mod.getVersion());
    }

    @Override
    @Deprecated
    public boolean isUpdated() {
        return this.change < 0;
    }

    public boolean isUpgraded() {
        return this.change < 0;
    }

    public boolean isDowngraded() {
        return this.change > 0;
    }

    public boolean isDifferent() {
        return this.change != 0;
    }
}
