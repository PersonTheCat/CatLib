package personthecat.catlib.data;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static personthecat.catlib.util.PathUtils.extension;

@SuppressWarnings("unused")
public enum JsonType {
    JSON("json", "mcmeta"),
    HJSON("hjson", "cave"),
    UNSUPPORTED;

    private final List<String> extensions;

    JsonType(final String... extensions) {
        this.extensions = Arrays.asList(extensions);
    }

    public static JsonType get(final File f) {
        return get(extension(f));
    }

    public static JsonType get(final String ext) {
        for (final JsonType type : values()) {
            if (type.extensions.contains(ext)) {
                return type;
            }
        }
        return UNSUPPORTED;
    }

    public static boolean isSupported(final File f) {
        return get(f) != UNSUPPORTED;
    }

    public static boolean isSupported(final String ext) {
        return get(ext) != UNSUPPORTED;
    }

    public static boolean isJson(final File f) {
        return isJson(extension(f));
    }

    public static boolean isJson(final String ext) {
        return JSON.extensions.contains(ext);
    }

    public static boolean isHjson(final File f) {
        return isHjson(extension(f));
    }

    public static boolean isHjson(final String ext) {
        return HJSON.extensions.contains(ext);
    }

    public boolean isJson() {
        return this == JSON;
    }

    public boolean isHjson() {
        return this == HJSON;
    }
}
