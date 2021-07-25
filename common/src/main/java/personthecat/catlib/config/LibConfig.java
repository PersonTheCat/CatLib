package personthecat.catlib.config;

import personthecat.overwritevalidator.annotations.OverwriteTarget;

import java.util.function.Supplier;

@OverwriteTarget
public class LibConfig {
    public static final Supplier<Boolean> ENABLE_GLOBAL_LIB_COMMANDS = () -> true;
}
