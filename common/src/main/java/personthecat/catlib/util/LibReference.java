package personthecat.catlib.util;

import personthecat.catlib.data.ModDescriptor;
import personthecat.catlib.versioning.Version;

public class LibReference {

    public static final String MOD_ID = "@MOD_ID@";
    public static final String MOD_NAME = "@MOD_NAME@";
    public static final Version MOD_VERSION = Version.parse("@MOD_VERSION@");

    public static final ModDescriptor MOD_DESCRIPTOR =
        ModDescriptor.builder().modId(MOD_ID).name(MOD_NAME).version(MOD_VERSION)
            .configFolder(McUtils.getConfigDir()).build();

}
