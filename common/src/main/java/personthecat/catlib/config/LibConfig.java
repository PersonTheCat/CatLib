package personthecat.catlib.config;

import dev.architectury.injectables.annotations.ExpectPlatform;
import lombok.experimental.UtilityClass;
import personthecat.catlib.event.error.Severity;
import personthecat.catlib.exception.MissingOverrideException;

@UtilityClass
public class LibConfig {

    @ExpectPlatform
    public static void register() {
        throw new MissingOverrideException();
    }

    @ExpectPlatform
    public static boolean enableCatlibCommands() {
        return true;
    }

    @ExpectPlatform
    public static Severity errorLevel() {
        return Severity.ERROR;
    }

    @ExpectPlatform
    public static boolean wrapText() {
        return true;
    }

    @ExpectPlatform
    public static int displayLength() {
        return 35;
    }

    @ExpectPlatform
    public static boolean enableTestError() {
        return false;
    }
}
