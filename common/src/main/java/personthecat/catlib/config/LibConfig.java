package personthecat.catlib.config;

import personthecat.catlib.event.error.Severity;
import personthecat.overwritevalidator.annotations.OverwriteTarget;

@OverwriteTarget
public class LibConfig {

    public static boolean enableGlobalLibCommands() {
        return true;
    }

    public static Severity getErrorLevel() {
        return Severity.ERROR;
    }
}
