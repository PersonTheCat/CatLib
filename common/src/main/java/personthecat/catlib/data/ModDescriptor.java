package personthecat.catlib.data;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.util.McUtils;
import personthecat.catlib.linting.SyntaxLinter;
import personthecat.catlib.versioning.Version;

import java.io.File;
import java.util.Objects;

/**
 * A platform-agnostic DTO containing information about a given mod's
 * ID, name, and folder layout. This will be used by the library for
 * generating commands and command arguments, but it may provide
 * additional functionalities in the future.
 * <p>
 *   Ideally, this object would not be constructed more than once.
 *   Consider creating this object in a static context and not allowing
 *   the reference to be modified at any point.
 * <p>
 *   This object makes use of {@link Builder} to provide a convenient
 *   builder API so that any number of arguments can be neatly applied.
 *   Consider adding a Lombok plugin to your build environment to avoid
 *   any generated code-related issues in your IDE.
 * <pre>
 *   public static final ModDescriptor MOD_DESCRIPTOR =
 *       ModDescriptor.builder()
 *           .name("Formatted Name")
 *           .modId("mod_id")
 *           .build();
 * </pre><p>
 *   Note that <code>name</code> and <code>modId</code> are the only
 *   required parameters. The others can be inferred if needed.
 * </p>
 */
@Builder
@SuppressWarnings("unused")
public record ModDescriptor(
    String name,
    String modId,
    Version version,
    String commandPrefix,
    File configFolder,
    File configFile,
    File backupFolder,
    @Nullable File preferredDirectory,
    SyntaxLinter defaultLinter) {

    public ResourceLocation id(final String path) {
        return new ResourceLocation(this.modId, path);
    }

    public static class ModDescriptorBuilder {
        public ModDescriptor build() {
            Objects.requireNonNull(this.name, "name must not be null");
            Objects.requireNonNull(this.modId, "modId must not be null");
            if (this.version == null) this.version = Version.ZERO;
            if (this.commandPrefix == null) this.commandPrefix = this.modId;
            if (this.configFolder == null) this.configFolder = new File(McUtils.getConfigDir(), this.modId);
            if (this.configFile == null) this.configFile = new File(McUtils.getConfigDir(), this.modId + ".djs");
            if (this.backupFolder == null) this.backupFolder = new File(this.configFolder, "backups");
            if (this.defaultLinter == null) this.defaultLinter = SyntaxLinter.DEFAULT_LINTER;

            return new ModDescriptor(name, modId, version, commandPrefix, configFolder, configFile, backupFolder, preferredDirectory, defaultLinter);
        }
    }
}
