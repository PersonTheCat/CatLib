package personthecat.catlib.exception;

import net.minecraft.world.level.biome.Biome;

import java.util.Arrays;

import static personthecat.catlib.util.Shorthand.f;

public class BiomeTypeNotFoundException extends RuntimeException {
    public BiomeTypeNotFoundException(final String name) {
        super(createMessage(name));
    }

    private static String createMessage(final String name) {
        return f("There is no biome name {}. Valid choices: {}", name,
            Arrays.toString(Biome.BiomeCategory.values()));
    }
}
