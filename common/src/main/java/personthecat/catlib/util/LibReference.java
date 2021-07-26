package personthecat.catlib.util;

import personthecat.catlib.data.ModDescriptor;

public class LibReference {
    public static final String MOD_ID = "catlib";
    public static final String MOD_NAME = "CatLib";

    public static final ModDescriptor MOD_DESCRIPTOR =
        ModDescriptor.builder().modId(MOD_ID).name(MOD_NAME).configFolder(McUtils.getConfigDir()).build();
}
