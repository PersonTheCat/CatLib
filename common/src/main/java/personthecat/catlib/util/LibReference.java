package personthecat.catlib.util;

import personthecat.catlib.data.ModDescriptor;

import java.util.Arrays;
import java.util.List;

public class LibReference {
    public static final String MOD_ID = "catlib";
    public static final String MOD_NAME = "CatLib";

    public static final ModDescriptor MOD_DESCRIPTOR =
        ModDescriptor.builder().modId(MOD_ID).name(MOD_NAME).configFolder(McUtils.getConfigDir()).build();

    public static final List<String> JSON_EXTENSIONS = Arrays.asList("json", "mcmeta");
    public static final List<String> HJSON_EXTENSIONS = Arrays.asList("hjson", "cave");
}
