package personthecat.catlib.versioning;

import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.event.lifecycle.ClientReadyEvent;
import personthecat.catlib.event.world.CommonWorldEvent;

public class VersionTracker {

    /**
     * Convenience method for tracking mod updates. Call this method with your {@link ModDescriptor}
     * to acquire a handle on the most recent {@link Version} of your mod.
     *
     * <p>Your mod's current version, provided by {@link ModDescriptor#getVersion}, will be persisted
     * on {@link ClientReadyEvent#EVENT} on the client side, or {@link CommonWorldEvent#LOAD} on
     * the server side.
     *
     * <p>For example, to check whether your mod has been updated by the user, call
     * {@link ConfigTracker#isUpdated}, as follows:
     * <pre>{@code
     *   VersionTracker.trackModVersion(MOD_DESCRIPTOR).isUpdated();
     * }</pre>
     *
     * <p>Additionally, you can use this handle to read the most recent version of your mod, as follows:
     * <pre>{@code
     *   final ConfigTracker&lt;Version&gt; versionCache = trackModVersion(MOD_DESCRIPTOR);
     *   final Version previousVersion = versionCache.getCacheOrUpdated();
     * }</pre>
     *
     * <p>It is highly recommended to only call this method a single time as it requires a bit of file
     * IO, which may be expensive.
     *
     * @param mod A descriptor providing details about your mod including its <b>current</b> version.
     * @return A tracker containing data about the last configured version of your mod.
     */
    public static ConfigTracker<Version> trackModVersion(final ModDescriptor mod) {
        return ConfigTracker.forMod(mod).withCategory("version")
            .scheduleSave(ConfigTracker.PersistOption.MAIN_MENU).track(mod.getVersion());
    }
}
