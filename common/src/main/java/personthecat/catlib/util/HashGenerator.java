package personthecat.catlib.util;

import java.util.Random;

/**
 * Generates noise quickly. Useful when shape isn't so important.
 * <p>
 *   Thanks to FastNoise for a very similar algorithm!
 * </p>
 */
@SuppressWarnings("unused")
public class HashGenerator {

    private static final long X_MULTIPLE = 0x653;  // 1619
    private static final long Y_MULTIPLE = 0x7A69; // 31337
    private static final long Z_MULTIPLE = 0x1B3B; // 6971

    private static final long GENERAL_MULTIPLE = 0x5DEECE66DL; // 25214903917
    private static final long ADDEND = 0xBL;                   // 11
    private static final long MASK = 0xFFFFFFFFFFFFL;          // 281474976710656
    private static final long SCALE = 0x16345785D8A0000L;      // E18

    /** The scrambled seed to use for hash generation. */
    private final long seed;

    public HashGenerator(long seed) {
        this.seed = scramble(seed);
    }

    /** Similar to {@link Random}'s scramble method. */
    private static long scramble(long seed) {
        long newSeed = (seed ^ GENERAL_MULTIPLE) & MASK;
        return (newSeed * GENERAL_MULTIPLE + ADDEND) & MASK;
    }

    public double getHash(int x, int y, int z) {
        // Clone the seed to allow for reuse.
        long hash = this.seed;

        // Mask the value using x, y, and z.
        hash ^= x * X_MULTIPLE;
        hash ^= y * Y_MULTIPLE;
        hash ^= z * Z_MULTIPLE;

        // Scale it up.
        hash *= hash;

        return ((hash >> 13) ^ hash) / (double) SCALE;
    }
}