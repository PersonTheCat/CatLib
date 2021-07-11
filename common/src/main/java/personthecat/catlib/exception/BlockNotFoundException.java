package personthecat.catlib.exception;

public class BlockNotFoundException extends RuntimeException {
    public BlockNotFoundException(final String name) {
        super("There is no block named " + name);
    }
}
