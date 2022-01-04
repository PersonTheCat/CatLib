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

    public static final RandomChunkSelector DEFAULT = new RandomChunkSelector();

    private final long defaultSeed;

    protected RandomChunkSelector() {
        this.defaultSeed = 0L;
    }

    public RandomChunkSelector(final long defaultSeed) {
        this.defaultSeed = defaultSeed;
    }

    /**
     * Obtain a random value from the three inputs using HashGenerator.
     * The threshold reflects the probability of selection.
     *
     * @param seed The seed to use for this comparison.
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @return whether this chunk has been selected.
     */
    public boolean testCoordinates(final long seed, final int ID, final int x, final int y) {
        return HashGenerator.getHash(seed, x, y, ID) > DEFAULT_THRESHOLD;
    }

    /**
     * Variant of {@link #testCoordinates(long, int, int, int)} providing
     * a default seed.
     *
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @return whether this chunk has been selected.
     */
    public boolean testCoordinates(final int ID, final int x, final int y) {
        return HashGenerator.getHash(this.defaultSeed, x, y, ID) > DEFAULT_THRESHOLD;
    }

    /**
     * Variant of {@link #testCoordinates(long, int, int, int)} accepting a
     * custom threshold.
     *
     * @param seed The seed to use for this comparison.
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @param threshold The minimum accepted output from the hasher.
     * @return whether this chunk has been selected.
     */
    public boolean testCoordinates(final long seed, final int ID, final int x, final int y, final double threshold) {
        return HashGenerator.getHash(seed, x, y, ID) > threshold;
    }

    /**
     * Variant of {@link #testCoordinates(long, int, int, int, double)}
     * providing a default seed.
     *
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @param threshold The minimum accepted output from the hasher.
     * @return whether this chunk has been selected.
     */
    public boolean testCoordinates(final int ID, final int x, final int y, final double threshold) {
        return HashGenerator.getHash(this.defaultSeed, x, y, ID) > threshold;
    }

    /**
     * Returns a random probability when given a chunk's coordinates and
     * a unique identifier.
     *
     * @param seed The seed to use for this comparison.
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @return A 0-1 probability representing a spawn chance for this chunk.
     */
    public double getProbability(final long seed, final int ID, final int x, final int y) {
        return getProbability(seed, ID, x, y, DEFAULT_THRESHOLD);
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
     * @param seed The seed to use for RNG.
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @param threshold The minimum accepted output from the hasher.
     * @return A 0-1 probability representing a spawn chance for this chunk.
     */
    public double getProbability(final long seed, final int ID, final int x, final int y, final double threshold) {
        if (testCoordinates(seed, ID, x, y, threshold)) {
            return MAX_PROBABILITY;
        }
        for (int i = 1; i <= DISTANCE; i++) {
            if (testDistance(seed, ID, x, y, i, threshold)) {
                // (0.8) -> 0.4 -> 0.2 -> etc.
                return (double) ((int) (MAX_PROBABILITY * 100) >> i) / 100.0;
            }
        }
        return DEFAULT_PROBABILITY;
    }

    /**
     * Variant of {@link #getProbability(long, int, int, int, double)} providing
     * a default seed.
     *
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @param threshold The minimum accepted output from the hasher.
     * @return A 0-1 probability representing a spawn chance for this chunk.
     */
    public double getProbability(final int ID, final int x, final int y, final double threshold) {
        return getProbability(this.defaultSeed, ID, x, y, threshold);
    }

    /**
     * Scans (most of) the surrounding chunks +- radius.
     *
     * @param seed The used to use for this comparison.
     * @param ID A unique identifier to further scramble the output.
     * @param x The chunk's x-coordinate.
     * @param y The chunk's y-coordinate.
     * @param radius The radius outward to scan.
     * @param threshold The minimum accepted output from the hasher.
     * @return Whether an matches were found.
     */
    private boolean testDistance(final long seed, final int ID, final int x, final int y, final int radius, final double threshold) {
        final int diameter = (radius * 2) + 1;
        final int innerLength = diameter - 2;
        final int shift = -(radius - 1);

        // Test the corners;
        if (testCoordinates(seed, ID, x + radius, y + radius)
            || testCoordinates(seed, ID, x - radius, y - radius)
            || testCoordinates(seed, ID, x + radius, y - radius)
            || testCoordinates(seed, ID, x - radius, y + radius)) {
            return true;
        }
        // Test the sides.
        for (int i = shift; i < innerLength + shift; i++) {
            if (testCoordinates(seed, ID, x + radius, y + i, threshold)
                || testCoordinates(seed, ID, x + i, y + radius, threshold)
                || testCoordinates(seed, ID, x - radius, y + i, threshold)
                || testCoordinates(seed, ID, x + i, y - radius, threshold)) {
                return true;
            }
        }
        return false;
    }

}
