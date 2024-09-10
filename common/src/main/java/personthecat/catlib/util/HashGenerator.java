package personthecat.catlib.util;

/**
 * Generates noise quickly. Useful when shape isn't so important.
 * <p>
 *   Thanks to FastNoise for a very similar algorithm!
 * </p>
 */
public class HashGenerator {

    private static final long X_MULTIPLE = 0x653;  // 1619
    private static final long Y_MULTIPLE = 0x7A69; // 31337
    private static final long Z_MULTIPLE = 0x1B3B; // 6971

    private static final long SCALE = 0x16345785D8A0000L; // E18

    private HashGenerator() {}

    public static double getHash(final long seed, final int x, final int y, final int z) {
        // Clone the seed to allow for reuse.
        long hash = seed;

        // Mask the value using x, y, and z.
        hash ^= x * X_MULTIPLE;
        hash ^= y * Y_MULTIPLE;
        hash ^= z * Z_MULTIPLE;

        // Scale it up.
        hash *= hash;

        return ((hash >> 13) ^ hash) / (double) SCALE;
    }
}