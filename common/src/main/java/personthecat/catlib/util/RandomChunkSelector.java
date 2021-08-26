package personthecat.catlib.util;

/**
 * The original hashing algorithm from OSV 1.12.
 * <p>
 *   Note that this algorithm produces a very obvious pattern of directionality.
 *   In many cases, this pattern <em>is</em> desirable, as tends to produce
 *   clusters in massive regions.
 * </p>
 * <p>
 *   However, there is likely a much simpler solution which may replace this code
 *   at some point in the future. As a result, its behavior could be slightly
 *   unstable.
 * </p>
 */
@SuppressWarnings("unused")
public class RandomChunkSelector {

    /** The hashing algorithm to be used for selecting chunks. */
    private final HashGenerator noise;

    /** Reflects the probability of selection for any given chunk. */
    private static final double MAX_THRESHOLD = 91.0; // Highest possible value.

    /** The default threshold to use when none is provided. */
    private static final double DEFAULT_THRESHOLD = MAX_THRESHOLD * 0.75;

    /** Highest possible probability out of 1 in selected chunks. */
    private static final double MAX_PROBABILITY = 0.8;

    /** Lowest possible probability out of 1 for any given chunk. */
    private static final double DEFAULT_PROBABILITY = 0.001;

    /** The radius of chunks to search outward. */
    private static final int DISTANCE = 2;

    public RandomChunkSelector(final long worldSeed) {
        this.noise = new HashGenerator(worldSeed);
    }

    /**
     * Obtain a random value from the three inputs using HashGenerator.
     * The threshold reflects the probability of selection.
     *
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @return whether this chunk has been selected.
     */
    public boolean testCoordinates(final int ID, final int x, final int y) {
        return this.noise.getHash(x, y, ID) > DEFAULT_THRESHOLD;
    }

    /**
     * Variant of {@link #testCoordinates(int, int, int)} accepting a
     * custom threshold.
     *
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @param threshold The minimum accepted output from the hasher.
     * @return whether this chunk has been selected.
     */
    public boolean testCoordinates(final int ID, final int x, final int y, final double threshold) {
        return this.noise.getHash(x, y, ID) > threshold;
    }

    /**
     * Returns a random probability when given a chunk's coordinates and
     * a unique identifier.
     *
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @return A 0-1 probability representing a spawn chance for this chunk.
     */
    public double getProbability(final int ID, final int x, final int y) {
        return getProbability(ID, x, y, DEFAULT_THRESHOLD);
    }

    /**
     * Variant of {@link #getProbability(int, int, int)} accepting a
     * custom threshold.
     *
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @param threshold The minimum accepted output from the hasher.
     * @return A 0-1 probability representing a spawn chance for this chunk.
     */
    public double getProbability(final int ID, final int x, final int y, final double threshold) {
        if (testCoordinates(ID, x, y, threshold)) {
            return MAX_PROBABILITY;
        }
        for (int i = 1; i <= DISTANCE; i++) {
            if (testDistance(ID, x, y, i, threshold)) {
                // (0.8) -> 0.4 -> 0.2 -> etc.
                return (double) ((int) (MAX_PROBABILITY * 100) >> i) / 100.0;
            }
        }
        return DEFAULT_PROBABILITY;
    }

    /**
     * Scans (most of) the surrounding chunks +- radius.
     *
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @param radius The radius outward to scan.
     * @param threshold The minimum accepted output from the hasher.
     * @return Whether an matches were found.
     */
    private boolean testDistance(final int ID, final int x, final int y, final int radius, final double threshold) {
        final int diameter = (radius * 2) + 1;
        final int innerLength = diameter - 2;
        final int shift = -(radius - 1);

        // Test the corners;
        if (testCoordinates(ID, x + radius, y + radius)
            || testCoordinates(ID, x - radius, y - radius)
            || testCoordinates(ID, x + radius, y - radius)
            || testCoordinates(ID, x - radius, y + radius)) {
            return true;
        }
        // Test the sides.
        for (int i = shift; i < innerLength + shift; i++) {
            if (testCoordinates(ID, x + radius, y + i, threshold)
                || testCoordinates(ID, x + i, y + radius, threshold)
                || testCoordinates(ID, x - radius, y + i, threshold)
                || testCoordinates(ID, x + i, y - radius, threshold)) {
                return true;
            }
        }
        return false;
    }

}
