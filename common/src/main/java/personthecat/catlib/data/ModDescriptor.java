package personthecat.catlib.data;

import lombok.Builder;
import lombok.Value;
import personthecat.catlib.util.McTools;

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
 *   Note that <code>name</code> and <code>modId</code> are the only\
 *   required parameters. The others can be inferred if needed.
 * </p>
 */
@Value
@Builder
@SuppressWarnings("unused")
public class ModDescriptor {
    String name;
    String modId;
    String commandPrefix;
    File configFolder;
    File backupFolder;

    public static class ModDescriptorBuilder {
        public ModDescriptor build() {
            Objects.requireNonNull(this.name, "name must not be null");
            Objects.requireNonNull(this.modId, "modId must not be null");
            if (this.commandPrefix == null) this.commandPrefix = this.modId;
            if (this.configFolder == null) this.configFolder = new File(McTools.getConfigDir(), this.modId);
            if (this.backupFolder == null) this.backupFolder = new File(this.configFolder, "backups");

            return new ModDescriptor(this.name, this.modId, this.commandPrefix, this.configFolder, this.backupFolder);
        }
    }
}
