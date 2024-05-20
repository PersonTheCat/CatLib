package personthecat.catlib.exception;

import personthecat.catlib.data.BiomeType;

import java.util.Arrays;

import static personthecat.catlib.util.LibUtil.f;

public class BiomeTypeNotFoundException extends RuntimeException {
    public BiomeTypeNotFoundException(final String name) {
        super(createMessage(name));
    }

    private static String createMessage(final String name) {
        return f("There is no biome name {}. Valid choices: {}", name,
            Arrays.toString(BiomeType.values()));
    }
}
