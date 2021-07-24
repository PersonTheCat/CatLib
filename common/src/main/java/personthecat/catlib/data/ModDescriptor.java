package personthecat.catlib.data;

import lombok.Builder;
import lombok.Value;
import personthecat.catlib.util.McTools;

import java.io.File;
import java.util.Objects;

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
